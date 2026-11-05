package org.north.core;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ArchitectureScanner {

    @Test
    public void viewArchitecture() {
        String basePackage = "org.north.core";

        System.out.println("Инициализация временного контекста OpenGL для безопасного сканирования...");
        long windowHandle = initTemporaryGLContext();

        try {
            List<Class<?>> classes = getClasses(basePackage);
            System.out.println("=== СКАНИРОВАНИЕ АРХИТЕКТУРЫ ДВИЖКА ===");
            System.out.println("Найдено классов: " + classes.size());
            System.out.println("======================================\n");

            for (Class<?> clazz : classes) {
                // Пропускаем сам сканер, чтобы не засорять вывод
                if (clazz == ArchitectureScanner.class) continue;

                printClassArchitecture(clazz);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при сканировании проекта: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Освобождаем контекст после сканирования
            if (windowHandle != 0) {
                GLFW.glfwDestroyWindow(windowHandle);
                GLFW.glfwTerminate();
                System.out.println("Временный контекст OpenGL успешно закрыт.");
            }
        }
    }

    // Создает невидимое окно и контекст, чтобы вызовы OpenGL внутри статических блоков не рушили JVM
    private static long initTemporaryGLContext() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Не удалось инициализировать GLFW");
        }
        // Делаем окно невидимым, оно нужно только ради контекста
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        long window = GLFW.glfwCreateWindow(1, 1, "Temp", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            throw new RuntimeException("Не удалось создать временное окно GLFW");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities(); // Инициализируем функции OpenGL для текущего потока
        return window;
    }



    private static void printClassArchitecture(Class<?> clazz) {
        // Определение типа (класс, интерфейс, enum, абстрактный класс)
        String typeKeyword = "class";
        if (clazz.isInterface()) typeKeyword = "interface";
        else if (clazz.isEnum()) typeKeyword = "enum";
        else if (Modifier.isAbstract(clazz.getModifiers())) typeKeyword = "abstract class";

        System.out.println(Modifier.toString(clazz.getModifiers() & ~Modifier.ABSTRACT) + " " + typeKeyword + " " + clazz.getName());

        // Наследование
        if (clazz.getSuperclass() != Object.class && clazz.getSuperclass() != null) {
            System.out.println("  extends " + clazz.getSuperclass().getSimpleName());
        }

        // Интерфейсы
        if (clazz.getInterfaces().length > 0) {
            System.out.print("  implements ");
            for (int i = 0; i < clazz.getInterfaces().length; i++) {
                System.out.print(clazz.getInterfaces()[i].getSimpleName());
                if (i < clazz.getInterfaces().length - 1) System.out.print(", ");
            }
            System.out.println();
        }

        // Поля класса (Публичные и протектед - важные для понимания состояния)
        Field[] fields = clazz.getDeclaredFields();
        boolean hasVisibleFields = false;
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                if (!hasVisibleFields) {
                    System.out.println("  FIELDS:");
                    hasVisibleFields = true;
                }
                System.out.println("    " + Modifier.toString(field.getModifiers()) + " " + field.getType().getSimpleName() + " " + field.getName());
            }
        }

        // Методы класса (API)
        System.out.println("  METHODS (API):");
        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length == 0) {
            System.out.println("    (нет методов в этом классе)");
        }
        for (Method method : methods) {
            // Показываем только public и protected, так как private — внутренняя кухня
            if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                System.out.print("    " + Modifier.toString(method.getModifiers()) + " " + method.getReturnType().getSimpleName() + " " + method.getName() + "(");

                Parameter[] params = method.getParameters();
                for (int i = 0; i < params.length; i++) {
                    System.out.print(params[i].getType().getSimpleName() + " " + params[i].getName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }
        }
        System.out.println("\n-----------------------------------------------------------------------\n");
    }

    // Вспомогательный метод для автоматического поиска всех классов в пакете
    private static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getPath().toString().replaceAll("%20", " ")));
        }
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}

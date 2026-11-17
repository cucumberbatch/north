package io.github.cucumberbatch.north.architecture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    private ApplicationContext context;

    private <T> T reg(Class<T> aClass) {
        return context.addDependency(aClass);
    }

    @BeforeEach
    public void init() {
        this.context = new ApplicationContext();
    }

    @Test
    public void testNoArgsConstructor() {
        assertNotNull(assertDoesNotThrow(() -> reg(NoArgsConstructorClass.class)));
    }

    @Test
    public void testNoArgsInjectConstructor() {
        assertNotNull(assertDoesNotThrow(() -> reg(NoArgsInjectConstructorClass.class)));
    }

    @Test
    public void testDependencyInjection() {
        C c = assertDoesNotThrow(() -> reg(C.class));
        B b = assertDoesNotThrow(() -> reg(B.class));
        A a = assertDoesNotThrow(() -> reg(A.class));
        All all = assertDoesNotThrow(() -> reg(All.class));

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertNotNull(all);
        assertNotNull(a.b);
        assertNotNull(b.c);
        assertNotNull(all.a);
        assertNotNull(all.b);
        assertNotNull(all.c);
        assertSame(a.b, b);
        assertSame(b.c, c);
        assertSame(a.b.c, c);
        assertSame(all.a.b.c, c);
    }

    private static class NoArgsConstructorClass {
        NoArgsConstructorClass() {}
    }

    private static class NoArgsInjectConstructorClass {
        @Inject NoArgsInjectConstructorClass() {}
    }

    private static class A {
        B b;
        @Inject A(B b) { this.b = b; }
    }

    private static class B {
        C c;
        @Inject B(C c) { this.c = c; }
    }

    private static class C {
        C() {}
    }

    private static class All {
        A a;
        B b;
        C c;

        @Inject
        All(A a, B b, C c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}

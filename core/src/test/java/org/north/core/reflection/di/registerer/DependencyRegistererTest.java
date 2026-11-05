package org.north.core.reflection.di.registerer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.north.core.architecture.DependencyRegisterer;
import org.north.core.architecture.Inject;

class DependencyRegistererTest {

    private DependencyRegisterer registerer;

    private <T> T reg(Class<T> aClass) throws ReflectiveOperationException {
        return registerer.registerDependency(aClass);
    }

    @BeforeEach
    public void init() {
        this.registerer = new DependencyRegisterer();
    }

    @Test
    public void testDependencyInjection1() {
        C c = Assertions.assertDoesNotThrow(() -> reg(C.class));
        B b = Assertions.assertDoesNotThrow(() -> reg(B.class));
        A a = Assertions.assertDoesNotThrow(() -> reg(A.class));
        All all = Assertions.assertDoesNotThrow(() -> reg(All.class));

        Assertions.assertNotNull(a);
        Assertions.assertNotNull(b);
        Assertions.assertNotNull(c);
        Assertions.assertNotNull(all);
        Assertions.assertNotNull(a.b);
        Assertions.assertNotNull(b.c);
        Assertions.assertNotNull(all.a);
        Assertions.assertNotNull(all.b);
        Assertions.assertNotNull(all.c);
        Assertions.assertSame(a.b, b);
        Assertions.assertSame(b.c, c);
        Assertions.assertSame(a.b.c, c);
        Assertions.assertSame(all.a.b.c, c);
    }

    @Test
    public void testDependencyInjection2() {
        Class<?>[] classes = {All.class, A.class, C.class, B.class};
        Object[] objects = Assertions.assertDoesNotThrow(() -> registerer.registerDependencies(classes));

        Assertions.assertNotNull(objects);
        Assertions.assertEquals(4, objects.length);

        Object _all = objects[0];
        Object _a = objects[1];
        Object _c = objects[2];
        Object _b = objects[3];

        Assertions.assertNotNull(_a);
        Assertions.assertNotNull(_b);
        Assertions.assertNotNull(_c);
        Assertions.assertNotNull(_all);
        Assertions.assertInstanceOf(A.class, _a);
        Assertions.assertInstanceOf(B.class, _b);
        Assertions.assertInstanceOf(C.class, _c);
        Assertions.assertInstanceOf(All.class, _all);

        A a = (A) _a;
        B b = (B) _b;
        C c = (C) _c;
        All all = (All) _all;

        Assertions.assertNotNull(a.b);
        Assertions.assertNotNull(b.c);
        Assertions.assertNotNull(all.a);
        Assertions.assertNotNull(all.b);
        Assertions.assertNotNull(all.c);
        Assertions.assertSame(a.b, b);
        Assertions.assertSame(b.c, c);
        Assertions.assertSame(a.b.c, c);
        Assertions.assertSame(all.a.b.c, c);
    }

    private static class A {
        B b;

        @Inject
        A(B b) {
            this.b = b;
        }
    }

    private static class B {
        C c;

        @Inject
        B(C c) {
            this.c = c;
        }
    }

    private static class C {
        C() {
        }
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
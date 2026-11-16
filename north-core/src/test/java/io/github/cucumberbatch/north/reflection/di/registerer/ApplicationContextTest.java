package io.github.cucumberbatch.north.reflection.di.registerer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.Inject;

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

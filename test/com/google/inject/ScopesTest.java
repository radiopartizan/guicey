/**
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject;

import com.google.common.collect.Maps;
import static com.google.inject.Asserts.assertContains;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * @author crazybob@google.com (Bob Lee)
 */
public class ScopesTest extends TestCase {

  private final AbstractModule singletonsModule = new AbstractModule() {
    protected void configure() {
      bind(BoundAsSingleton.class).in(Scopes.SINGLETON);
      bind(AnnotatedSingleton.class);
      bind(EagerSingleton.class).asEagerSingleton();
      bind(LinkedSingleton.class).to(RealLinkedSingleton.class);
      bind(DependsOnJustInTimeSingleton.class);
      bind(NotASingleton.class);
      bind(ImplementedBySingleton.class).in(Scopes.SINGLETON);
      bind(ProvidedBySingleton.class).in(Scopes.SINGLETON);
    }
  };

  @Override protected void setUp() throws Exception {
    AnnotatedSingleton.nextInstanceId = 0;
    BoundAsSingleton.nextInstanceId = 0;
    EagerSingleton.nextInstanceId = 0;
    RealLinkedSingleton.nextInstanceId = 0;
    JustInTimeSingleton.nextInstanceId = 0;
    NotASingleton.nextInstanceId = 0;
    Implementation.nextInstanceId = 0;
    ProvidedBySingleton.nextInstanceId = 0;
  }

  public void testSingletons() {
    Injector injector = Guice.createInjector(singletonsModule);

    assertSame(
        injector.getInstance(BoundAsSingleton.class),
        injector.getInstance(BoundAsSingleton.class));

    assertSame(
        injector.getInstance(AnnotatedSingleton.class),
        injector.getInstance(AnnotatedSingleton.class));

    assertSame(
        injector.getInstance(EagerSingleton.class),
        injector.getInstance(EagerSingleton.class));

    assertSame(
        injector.getInstance(LinkedSingleton.class),
        injector.getInstance(LinkedSingleton.class));

    assertSame(
        injector.getInstance(JustInTimeSingleton.class),
        injector.getInstance(JustInTimeSingleton.class));

    assertNotSame(
        injector.getInstance(NotASingleton.class),
        injector.getInstance(NotASingleton.class));

    assertSame(
        injector.getInstance(ImplementedBySingleton.class),
        injector.getInstance(ImplementedBySingleton.class));

    assertSame(
        injector.getInstance(ProvidedBySingleton.class),
        injector.getInstance(ProvidedBySingleton.class));
  }

  public void testJustInTimeAnnotatedSingleton() {
    Injector injector = Guice.createInjector();

    assertSame(
        injector.getInstance(AnnotatedSingleton.class),
        injector.getInstance(AnnotatedSingleton.class));
  }

  public void testSingletonIsPerInjector() {
    assertNotSame(
        Guice.createInjector().getInstance(AnnotatedSingleton.class),
        Guice.createInjector().getInstance(AnnotatedSingleton.class));
  }

  public void testOverriddingAnnotation() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(AnnotatedSingleton.class).in(Scopes.NO_SCOPE);
      }
    });

    assertNotSame(
        injector.getInstance(AnnotatedSingleton.class),
        injector.getInstance(AnnotatedSingleton.class));
  }

  public void testScopingAnnotationsOnAbstractTypeViaBind() {
    try {
      Guice.createInjector(new AbstractModule() {
        protected void configure() {
          bind(A.class).to(AImpl.class);
        }
      });
      fail();
    } catch (CreationException expected) {
      assertContains(expected.getMessage(),
          A.class.getName() + " is annotated with " + Singleton.class.getName(),
          "but scope annotations are not supported for abstract types.",
          "at " + A.class.getName() + ".class(ScopesTest.java:");
    }
  }

  @Singleton
  interface A {}
  static class AImpl implements A {}

  public void testScopingAnnotationsOnAbstractTypeViaImplementedBy() {
    try {
      Guice.createInjector().getInstance(D.class);
      fail();
    } catch (ConfigurationException expected) {
      assertContains(expected.getMessage(),
          D.class.getName() + " is annotated with " + Singleton.class.getName(),
          "but scope annotations are not supported for abstract types.",
          "at " + D.class.getName() + ".class(ScopesTest.java:");
    }
  }

  @Singleton @ImplementedBy(DImpl.class)
  interface D {}
  static class DImpl implements D {}

  public void testScopingAnnotationsOnAbstractTypeViaProvidedBy() {
    try {
      Guice.createInjector().getInstance(E.class);
      fail();
    } catch (ConfigurationException expected) {
      assertContains(expected.getMessage(),
          E.class.getName() + " is annotated with " + Singleton.class.getName(),
          "but scope annotations are not supported for abstract types.",
          "at " + E.class.getName() + ".class(ScopesTest.java:");
    }
  }

  @Singleton @ProvidedBy(EProvider.class)
  interface E {}
  static class EProvider implements Provider<E> {
    public E get() {
      return null;
    }
  }

  public void testScopeUsedButNotBound() {
    try {
      Guice.createInjector(new AbstractModule() {
        protected void configure() {
          bind(B.class).in(CustomScoped.class);
          bind(C.class);
        }
      });
      fail();
    } catch (CreationException expected) {
      assertContains(expected.getMessage(),
          "1) No scope is bound to " + CustomScoped.class.getName(),
          "at " + getClass().getName(), ".configure(ScopesTest.java:",
          "2) No scope is bound to " + CustomScoped.class.getName(),
          "at " + C.class.getName() + ".class");
    }
  }

  static class B {}

  @CustomScoped
  static class C {}

  public void testSingletonsInProductionStage() {
    Guice.createInjector(Stage.PRODUCTION, singletonsModule);

    assertEquals(1, AnnotatedSingleton.nextInstanceId);
    assertEquals(1, BoundAsSingleton.nextInstanceId);
    assertEquals(1, EagerSingleton.nextInstanceId);
    assertEquals(1, RealLinkedSingleton.nextInstanceId);
    assertEquals(1, JustInTimeSingleton.nextInstanceId);
    assertEquals(0, NotASingleton.nextInstanceId);
  }

  public void testSingletonsInDevelopmentStage() {
    Guice.createInjector(Stage.DEVELOPMENT, singletonsModule);

    assertEquals(0, AnnotatedSingleton.nextInstanceId);
    assertEquals(0, BoundAsSingleton.nextInstanceId);
    assertEquals(1, EagerSingleton.nextInstanceId);
    assertEquals(0, RealLinkedSingleton.nextInstanceId);
    assertEquals(0, JustInTimeSingleton.nextInstanceId);
    assertEquals(0, NotASingleton.nextInstanceId);
  }

  public void testSingletonScopeIsNotSerializable() throws IOException {
    Asserts.assertNotSerializable(Scopes.SINGLETON);
  }

  public void testNoScopeIsNotSerializable() throws IOException {
    Asserts.assertNotSerializable(Scopes.NO_SCOPE);
  }

  public void testUnscopedProviderWorksOutsideOfRequestedScope() {
    final RememberProviderScope scope = new RememberProviderScope();
    
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bindScope(CustomScoped.class, scope);
        bind(List.class).to(ArrayList.class).in(CustomScoped.class);
      }
    });

    injector.getInstance(List.class);
    Provider<?> listProvider = scope.providers.get(Key.get(List.class));

    // this line fails with a NullPointerException because the Providers
    // passed to Scope.scope() don't work outside of the scope() method.
    assertTrue(listProvider.get() instanceof ArrayList);
  }

  public void testScopeAnnotationWithoutRuntimeRetention() {
    try {
      Guice.createInjector(new AbstractModule() {
        protected void configure() {
          bindScope(NotRuntimeRetainedScoped.class, Scopes.NO_SCOPE);
        }
      });
      fail();
    } catch (CreationException expected) {
      assertContains(expected.getMessage(),
          "1) Please annotate with @Retention(RUNTIME).",
          "at " + NotRuntimeRetainedScoped.class.getName() + ".class(ScopesTest.java:");
    }
  }

  public void testBindScopeToAnnotationWithoutScopeAnnotation() {
    try {
      Guice.createInjector(new AbstractModule() {
        protected void configure() {
          bindScope(Deprecated.class, Scopes.NO_SCOPE);
        }
      });
      fail();
    } catch (CreationException expected) {
      assertContains(expected.getMessage(),
          "1) Please annotate with @ScopeAnnotation.",
          "at " + Deprecated.class.getName() + ".class(");
    }
  }

  public void testBindScopeTooManyTimes() {
    try {
      Guice.createInjector(new AbstractModule() {
        protected void configure() {
          bindScope(CustomScoped.class, Scopes.NO_SCOPE);
          bindScope(CustomScoped.class, Scopes.SINGLETON);
        }
      });
      fail();
    } catch (CreationException expected) {
      assertContains(expected.getMessage(),
          "1) Scope Scopes.NO_SCOPE is already bound to " + CustomScoped.class.getName(),
          "Cannot bind Scopes.SINGLETON.",
          "at " + ScopesTest.class.getName(), ".configure(ScopesTest.java:");
    }
  }

  public void testDuplicateScopeAnnotations() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bindScope(CustomScoped.class, Scopes.NO_SCOPE);
      }
    });

    try {
      injector.getInstance(SingletonAndCustomScoped.class);
      fail();
    } catch (ConfigurationException expected) {
      assertContains(expected.getMessage(),
          "1) More than one scope annotation was found: ",
          "while locating " + SingletonAndCustomScoped.class.getName());
    }
  }

  public void testNullScopedAsASingleton() {
    Provider<String> unscoped = new Provider<String>() {
      final Iterator<String> values = Arrays.asList(null, "A").iterator();
      public String get() {
        return values.next();
      }
    };

    Provider<String> scoped = Scopes.SINGLETON.scope(Key.get(String.class), unscoped);
    assertNull(scoped.get());
    assertNull(scoped.get());
    assertNull(scoped.get());
  }

  class RememberProviderScope implements Scope {
    final Map<Key<?>, Provider<?>> providers = Maps.newHashMap();
    public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
      providers.put(key, unscoped);
      return unscoped;
    }
  }

  public void testSingletonAnnotationOnParameterizedType() {
    Injector injector = Guice.createInjector();
    assertSame(injector.getInstance(new Key<Injected<String>>() {}),
        injector.getInstance(new Key<Injected<String>>() {}));
    assertSame(injector.getInstance(new Key<In<Integer>>() {}),
        injector.getInstance(new Key<In<Short>>() {}));
  }

  @ImplementedBy(Injected.class) public interface In<T> {}
  @Singleton public static class Injected<T>  implements In<T> {}

  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Retention(RUNTIME)
  @ScopeAnnotation
  public @interface CustomScoped {}

  @Target({ ElementType.TYPE, ElementType.METHOD })
  @ScopeAnnotation
  public @interface NotRuntimeRetainedScoped {}

  @Singleton
  static class AnnotatedSingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class BoundAsSingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class EagerSingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  interface LinkedSingleton {}

  @Singleton
  static class RealLinkedSingleton implements LinkedSingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class DependsOnJustInTimeSingleton {
    @Inject JustInTimeSingleton justInTimeSingleton;
  }

  @Singleton
  static class JustInTimeSingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class NotASingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  @Singleton @CustomScoped
  static class SingletonAndCustomScoped {}

  @ImplementedBy(Implementation.class)
  static interface ImplementedBySingleton {}

  @ProvidedBy(ImplementationProvider.class)
  static class ProvidedBySingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class Implementation implements ImplementedBySingleton {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
  }

  static class ImplementationProvider implements Provider<ProvidedBySingleton> {
    public ProvidedBySingleton get() {
      return new ProvidedBySingleton();
    }
  }
}

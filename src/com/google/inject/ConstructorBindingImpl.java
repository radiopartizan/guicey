/*
 * Copyright (C) 2007 Google Inc.
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

import com.google.inject.internal.BindingImpl;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;
import com.google.inject.internal.ImmutableSet;
import com.google.inject.internal.InternalContext;
import com.google.inject.internal.InternalFactory;
import static com.google.inject.internal.Preconditions.checkState;
import com.google.inject.internal.Scoping;
import com.google.inject.internal.ToStringBuilder;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.CachedValue;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ConstructorBindingImpl<T> extends BindingImpl<T> implements ConstructorBinding<T> {

  private final Factory<T> factory;

  private ConstructorBindingImpl(Injector injector, Key<T> key, Object source,
      InternalFactory<? extends T> scopedFactory, Scoping scoping, Factory<T> factory) {
    super(injector, key, source, scopedFactory, scoping);
    this.factory = factory;
  }

  static <T> ConstructorBindingImpl<T> create(
      InjectorImpl injector, Key<T> key, Object source, Scoping scoping) {
    Factory<T> factoryFactory = new Factory<T>();
    InternalFactory<? extends T> scopedFactory
        = Scopes.scope(key, injector, factoryFactory, scoping);
    return new ConstructorBindingImpl<T>(
        injector, key, source, scopedFactory, scoping, factoryFactory);
  }

  public void initialize(InjectorImpl injector, Errors errors) throws ErrorsException {
    factory.constructorInjector = injector.constructors.get(getKey().getTypeLiteral(), errors);
  }

  public <V> V acceptTargetVisitor(BindingTargetVisitor<? super T, V> visitor) {
    checkState(factory.constructorInjector != null, "not initialized");
    return visitor.visit(this);
  }

  public InjectionPoint getConstructor() {
    checkState(factory.constructorInjector != null, "Binding is not ready");
    return factory.constructorInjector.getConstructionProxy().getInjectionPoint();
  }

  public Set<InjectionPoint> getInjectableMembers() {
    checkState(factory.constructorInjector != null, "Binding is not ready");
    return factory.constructorInjector.getInjectableMembers();
  }

  /*if[AOP]*/
  public Map<Method, List<org.aopalliance.intercept.MethodInterceptor>> getMethodInterceptors() {
    checkState(factory.constructorInjector != null, "Binding is not ready");
    return factory.constructorInjector.getConstructionProxy().getMethodInterceptors();
  }
  /*end[AOP]*/

  public Set<Dependency<?>> getDependencies() {
    return Dependency.forInjectionPoints(new ImmutableSet.Builder<InjectionPoint>()
        .add(getConstructor())
        .addAll(getInjectableMembers())
        .build());
  }

  public void applyTo(Binder binder) {
    throw new UnsupportedOperationException("This element represents a synthetic binding.");
  }

  @Override public String toString() {
    return new ToStringBuilder(ConstructorBinding.class)
        .add("key", getKey())
        .add("source", getSource())
        .add("scope", getScoping())
        .toString();
  }

  private static class Factory<T> implements InternalFactory<T>, CachedValue<T> {
    private ConstructorInjector<T> constructorInjector;

    @SuppressWarnings("unchecked")
    public T get(Errors errors, InternalContext context, Dependency<?> dependency)
        throws ErrorsException {
      checkState(constructorInjector != null, "Constructor not ready");

      // This may not actually be safe because it could return a super type of T (if that's all the
      // client needs), but it should be OK in practice thanks to the wonders of erasure.
      return (T) constructorInjector.construct(errors, context, dependency.getKey().getRawType());
    }

    public T getCachedValue() {
      return constructorInjector.getCachedValue();
    }
  }
}

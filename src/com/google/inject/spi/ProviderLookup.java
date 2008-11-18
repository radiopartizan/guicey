/**
 * Copyright (C) 2008 Google Inc.
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

package com.google.inject.spi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.inject.Key;
import com.google.inject.Provider;

/**
 * A lookup of the provider for a type. Lookups are created explicitly in a module using
 * {@link com.google.inject.Binder#getProvider(Class) getProvider()} statements:
 * <pre>
 *     Provider&lt;PaymentService&gt; paymentServiceProvider
 *         = getProvider(PaymentService.class);</pre>
 *
 * @author jessewilson@google.com (Jesse Wilson)
 * @since 2.0
 */
public final class ProviderLookup<T> implements Element {
  private final Object source;
  private final Key<T> key;
  private Provider<T> delegate;

  ProviderLookup(Object source, Key<T> key) {
    this.source = checkNotNull(source, "source");
    this.key = checkNotNull(key, "key");
  }

  public Object getSource() {
    return source;
  }

  public Key<T> getKey() {
    return key;
  }

  public <T> T acceptVisitor(ElementVisitor<T> visitor) {
    return visitor.visitProviderLookup(this);
  }

  public void initDelegate(Provider<T> delegate) {
    checkState(this.delegate == null, "delegate already initialized");
    checkNotNull(delegate, "delegate");
    this.delegate = delegate;
  }

  /**
   * Returns the delegate provider, or {@code null} if it has not yet been initialized. The delegate
   * will be initialized when this element is processed, or otherwise used to create an injector.
   */
  public Provider<T> getDelegate() {
    return delegate;
  }
}

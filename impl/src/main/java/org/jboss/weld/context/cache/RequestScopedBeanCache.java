/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.context.cache;

import java.util.LinkedList;
import java.util.List;

/**
 * Caches beans over the life of a request, to allow for efficient bean lookups from proxies.
 *
 * @author Stuart Douglas
 */
public class RequestScopedBeanCache {

    private static final ThreadLocal<List<RequestScopedItem>> CACHE = new ThreadLocal<List<RequestScopedItem>>();

    public static boolean isActive() {
        return CACHE.get() != null;
    }

    public static void addItem(final RequestScopedItem item) {
        final List<RequestScopedItem> cache = CACHE.get();
        if (cache == null) {
            throw new IllegalStateException("Unable to add request scoped cache item when request cache is not active");
        }
        cache.add(item);
    }

    public static void addItem(final ThreadLocal item) {
        final List<RequestScopedItem> cache = CACHE.get();
        if (cache == null) {
            throw new IllegalStateException("Unable to add request scoped cache item when request cache is not active");
        }
        cache.add(new RequestScopedItem() {
            public void invalidate() {
                item.remove();
            }
        });
    }

    public static void beginRequest() {
        CACHE.set(new LinkedList<RequestScopedItem>());
    }

    /**
     * ends the request and clears the cache. This can be called before the request is over,
     * in which case the cache will be unavailable for the rest of the request.
     */
    public static void endRequest() {
        final List<RequestScopedItem> result = CACHE.get();
        CACHE.remove();
        if (result != null) {
            for (final RequestScopedItem item : result) {
                item.invalidate();
            }
        }
    }

}

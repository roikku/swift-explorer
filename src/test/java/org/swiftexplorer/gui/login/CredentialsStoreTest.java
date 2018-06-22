/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.swiftexplorer.gui.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import org.junit.Before;
import org.junit.Test;

public class CredentialsStoreTest {

    private CredentialsStore store;
    private CredentialsStore.Credentials cr;

    @Before
    public void init() {
        store = new CredentialsStore();
        cr = new CredentialsStore.Credentials();
        cr.authUrl = "https://42.nl";
        cr.tenant = "test-tenant";
        cr.username = "test-user";
        cr.password = "boterhammetpindakaas".toCharArray();
        cr.preferredRegion = "myregion1";
    }

    @Test
    public void shouldSaveReadAndDeleteCredentials() {
        int base = store.getAvailableCredentials().size();
        store.save(cr);
        try {
            assertEquals(base + 1, store.getAvailableCredentials().size());
            assertTrue(store.getAvailableCredentials().contains(cr));
        } finally {
            store.delete(cr);
        }
        assertEquals(base, store.getAvailableCredentials().size());
    }

    @Test
    public void shouldGarbleAndDeGarblePassword() {
        assertEquals("Pindakaas", String.valueOf(store.garble(String.valueOf(store.garble("Pindakaas")))));
        assertEquals("abcdefghijklmnopqrstuvwxyz", String.valueOf(store.garble(String.valueOf(store.garble("abcdefghijklmnopqrstuvwxyz")))));
    }
}

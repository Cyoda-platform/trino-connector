/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.core.util;
import org.joda.beans.ser.JodaBeanSer;

public class JodaBeanSerUtil {

    private static final JodaBeanSer COMPACT = JodaBeanSer.COMPACT.withIncludeDerived(true).withShortTypes(false);
    private static final JodaBeanSer PRETTY = JodaBeanSer.PRETTY.withIncludeDerived(true).withShortTypes(false);

    private JodaBeanSerUtil() {
    }

    public static JodaBeanSer compact() {
        return COMPACT;
    }

    public static JodaBeanSer pretty() {
        return PRETTY;
    }
}
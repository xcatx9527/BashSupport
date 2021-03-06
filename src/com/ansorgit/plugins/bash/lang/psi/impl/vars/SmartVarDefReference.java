/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.psi.impl.vars;

import com.ansorgit.plugins.bash.lang.psi.util.BashResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author jansorg
 */
final class SmartVarDefReference extends AbstractVarDefReference {

    public SmartVarDefReference(BashVarDefImpl bashVarDef) {
        super(bashVarDef);
    }

    @Nullable
    @Override
    public PsiElement resolveInner() {
        if (bashVarDef.isCommandLocal()) {
            return null;
        }

        return BashResolveUtil.resolve(bashVarDef, false, false);
    }
}

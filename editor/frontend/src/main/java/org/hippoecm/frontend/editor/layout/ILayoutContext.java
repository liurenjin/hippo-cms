/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor.layout;

import org.apache.wicket.IClusterable;

/**
 * Interface for layout aware services to control their position.
 */
public interface ILayoutContext extends IClusterable {

    /**
     * The layout pad for the layout aware service.
     *
     * @return the pad
     */
    ILayoutPad getLayoutPad();
    
    void apply(ILayoutTransition transition);
}
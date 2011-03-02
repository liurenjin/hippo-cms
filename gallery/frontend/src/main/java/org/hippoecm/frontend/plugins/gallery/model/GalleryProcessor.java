/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.gallery.model;

import java.awt.Dimension;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;

public interface GalleryProcessor extends IClusterable {
    final static String SVN_ID = "$Id$";

    public void makeImage(Node node, InputStream istream, String mimeType, String filename) throws GalleryException,
            RepositoryException;

    public void validateResource(Node node, String fileName) throws GalleryException, RepositoryException;

    public void initGalleryResource(Node node, InputStream data, String mimeType, String fileName, Calendar lastModified)
            throws GalleryException, RepositoryException;

    public Dimension getDesiredResourceDimension(Node node) throws GalleryException, RepositoryException;
}


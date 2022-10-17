/*
 * Copyright 2015-2019 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package grails.plugins.scim.exceptions

import groovy.transform.CompileStatic;


/**
 * Signals the specified resource; for example, User, does not exist.
 *
 * This exception corresponds to HTTP response code 404 NOT FOUND.
 */

@CompileStatic
public class ResourceNotFoundException extends Exception
{
  private static final long serialVersionUID = -1384059700223818255L;

  private static final String ID_ERROR_MESSAGE = "Resource %s not found";

  public ResourceNotFoundException(final int id) {
    this(String.format(ID_ERROR_MESSAGE, id));
  }

  /**
   * Create a new <code>ResourceNotFoundException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ResourceNotFoundException(final String errorMessage) {
    super(errorMessage);
  }



}

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

package grails.plugins.scim.exceptions;


import groovy.transform.CompileStatic;

/**
 * Signals the specified version number does not match the resource's latest
 * version number or a Service Provider refused to create a new,
 * duplicate resource.
 *
 * This exception corresponds to HTTP response code 409 CONFLICT.
 */
@CompileStatic
public class ResourceConflictException extends Exception
{

  private static final long serialVersionUID = -5605955982293892224L;

  /**
   * Create a new <code>ResourceConflictException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ResourceConflictException(final String errorMessage) {
    super(errorMessage);
  }

}

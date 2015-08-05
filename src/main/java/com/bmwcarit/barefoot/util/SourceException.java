/*
 * Copyright (C) 2015, BMW Car IT GmbH
 *
 * Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.bmwcarit.barefoot.util;


/**
 * Exception for handling any sort of data sources.
 */
public class SourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link SourceException} with an error message.
     *
     * @param message Message of the exception.
     */
    public SourceException(String message) {
        super(message);
    }

    /**
     * Creates a {@link SourceException} with an error message and stack trace.
     *
     * @param message Message of the exception.
     * @param stack_trace Stack trace of the exception, e.g. when wrapping another exception.
     */
    public SourceException(String message, StackTraceElement[] stack_trace) {
        super(message);
        super.setStackTrace(stack_trace);
    }
}

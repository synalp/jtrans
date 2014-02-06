/*
 * Copyright (c) 2011, Jonathan Keatley. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package fr.loria.synalp.jtrans.utils.spantable;

import java.util.EventObject;

/**
 * An event fired by a SpanModel when some change occurs to it.
 * @author Jonathan Keatley
 */
public class SpanEvent extends EventObject {
	private static final long serialVersionUID = -8615270194976006509L;
	private Span span;

	/**
	 * Constructs a SpanEvent.
	 * @param source The span model firing the event.
	 * @param span The span associated with the event.
	 * @throws IllegalArgumentException if source is null.
	 */
	public SpanEvent(SpanModel source, Span span) {
		super(source);
		this.span = span;
	}

	/**
	 * Constructs a SpanEvent with no span.
	 * @param source The span model firing the event.
	 */
	public SpanEvent(SpanModel source) {
		this(source, null);
	}

	public Span getSpan() {
		return span;
	}
}


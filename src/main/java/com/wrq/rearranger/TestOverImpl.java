/*
 * Copyright (c) 2003, 2010, Dave Kriewall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wrq.rearranger;

/**
 * Test method attributes: overriding, implementing, overridden, implemented.
 */
public abstract class TestOverImpl
		implements Runnable {

// ------------------------ CANONICAL METHODS ------------------------

	public String toString() {
		// overrides Object.toString()
		return super.toString();    //To change body of overridden methods use File | Settings | File Templates.
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Runnable ---------------------

	@Override
	public void run() {
		// implements Runnable.run()
	}

// -------------------------- OTHER METHODS --------------------------

	abstract void abstractMethod();   // is implemented

	abstract void isntImplementedMethod();

	void overrideableMethod() // is overridden
	{
		// do nothing
	}

}

abstract class TestOverImpl2 extends TestOverImpl {

// -------------------------- OTHER METHODS --------------------------

	@Override
	void abstractMethod()    // implements
	{
		// implements abstractMethod()
	}

	@Override
	void overrideableMethod()  // overrides
	{
		super.overrideableMethod();    //To change body of overridden methods use File | Settings | File Templates.
	}

}


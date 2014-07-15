/*
 * Copyright (c) 2012 the original author or authors.
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
 */
package org.grails.plugins.fixedflatfile
/**
 * Equals - verify of a set or parameters are equals to context values, if yes the inner closure 
 * is executed (parsed or formated)
 * @author oscar
 *
 */
class EqualCommand extends ControlCommand{
	def parameters
	
	String toString(){
		"eq(${parameters}) {\n${steps}\n}"
	}
	
	def satisfy(context){
		def r=true
		parameters.each { k,v->
			if (context[k]!=v) r=false
		}
		return r
	}
	
	def parse(String text, context,exec=true) {
		if (satisfy(context)) {
			steps.parse(text,context,exec)
			if(exec && closure)
				closure(context)
		}
		context
	}
	
	def generate(context,text=''){
		if (satisfy(context)) {
			def newText=text
			newText=steps.generate(context,newText)
			return newText
		} else {
			return text
		}
	}
}

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

class Steps {
	def name
	def commands=[]
	def closure
	
	String toString() {
		"${name ? name : 'steps'} {\n${commands.each{ it }}\n}"
	}
	def parse(text,context,exec=true) {
		commands.each {
			if (it instanceof ControlCommand)
				it.parse(text,context,exec)
			else it.parse(text,context)
		}
		if (exec && closure) {
			closure(context)
		}
		context
	}
	
	def generate(context,text=''){
		def newText=text
		commands.each {
			newText=it.generate(context,newText)
		}
		newText
	}

}

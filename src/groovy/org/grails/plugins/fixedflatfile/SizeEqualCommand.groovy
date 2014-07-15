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

class SizeEqualCommand extends ControlCommand{
	def size
	
	String toString(){
		"sizeEq(${size}) {\n${steps}\n}"
	}
	
	def satisfy(text){
		return text ? size==text.size() : false
	}
	
	def parse(String text, context,exec=true) {
		if (satisfy(text)) {
			println "Tamanho OK       : ${text.size()} [${size}]"
			steps.parse(text,context,exec)
			if(exec && closure) {
				println "Executando Closure"
				closure(context)
			}
		} else println "Tamanho diferente: ${text.size()} [${size}]"
		context
	}
	def generate(context,text=''){
		throw new RuntimeException("Invalid command to generate file")
	}
}

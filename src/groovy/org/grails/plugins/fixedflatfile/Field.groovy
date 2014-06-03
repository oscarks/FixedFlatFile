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
 * Field representation
 * @author oscar
 *
 */

class Field extends Command {
	int begin
	int end
	String name
	def notrim=false
	def closure
	def convert
	def type='A'
	def align='D'
	def format
	def truncate=false
	def defaultValue
	
	String toString() {
		"${name} ([${begin}..${end}]) {\n ${closure}\n}"
	}
	def parse(text,context){
		def v
		try {
			v=text.substring(begin-1,end)
			if (!notrim)
				v=v.trim()
			if (type) {
				switch(type.toUpperCase()) {
					case 'N':
					case 'NUMERIC':
					case 'NUMBER':
					case 'INTEGER':
						v=v.toInteger()
						break
				}
			}
			if (convert && convert instanceof Closure) {
				if (convert.maximumNumberOfParameters==2)
					v=convert(v,context)
				else v=convert(v)
			}
			context[name]=v
			if (closure) {
				closure(context)
			}
			context
		} catch(Exception e) {
			throw new FixedLenFileException("Error to parse field '${name}' [$begin-$end] = '${v}': ${e.message}",e)
		}
	}
	def generate(context,text=''){
		try {
			def val=context[name]
			if (val==null && defaultValue!=null) val=defaultValue
			if (format && format instanceof Closure) {
				if (format.maximumNumberOfParameters==2)
					val=format(val,context)
				else val=format(val)
			}
			if (val==null)
				throw new FixedLenFileException("No value or default for field ${name}")
				
			//put(text,val,begin,end)
			def size=end-begin+1
			text+formatField(val,size)
		} catch(Exception e) {
			throw new FixedLenFileException("Error to generate field '${name}': ${e.message}",e)
		}
	}
	
	
	String formatField(val,size) {
		def fillerChar
		def fieldAlign
		if (type=='N') fillerChar='0'
		else fillerChar=' '
		if (align=='D' && type=='N') fieldAlign='R'
		else if (align=='D' && type=='A') fieldAlign='L'
		else fieldAlign=align
		def fieldVal
		if (val==null) fieldVal=''
		else fieldVal=val.toString()
		if (fieldVal.size()>size) {
			if(!truncate) throw new RuntimeException("Value bigger then size of field $name (value: $val)")
			else if (fieldAlign=='R') fieldVal=fieldVal.substring(fieldVal.size()-size)
			else fieldVal=fieldVal.substring(0,size)
		} else {
			if (fieldAlign=='R') while(fieldVal.size()<size) fieldVal=fillerChar+fieldVal
			else  while(fieldVal.size()<size) fieldVal+=fillerChar
		}
		fieldVal
	}
	String put(s,val,i,f) {
		def o=s
		def inicio=i-1
		def fim=f-1
		def size=f-i+1
		def fchar=' '
		while(o.size()<i) o+=fchar
		def prefix=o.substring(0,inicio)
		def posfix
		if (o.size()>fim)
			posfix=o.substring(f)
			
		
		def r=prefix+ formatField(val,size)
		if (posfix) r=r+posfix
		r
	}

}

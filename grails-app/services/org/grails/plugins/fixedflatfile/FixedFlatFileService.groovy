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
 * @author oscar
 *
 */
class FixedFlatFileService {
	static transactional = false
	
    def createBuilder() {
		new FixedLenFileBuilder()
    }
	def build(definition) {
		def builder=new FixedLenFileBuilder()
		def d=builder.definition
		d
	}
	def parse(String filename, initialContext=[:], Closure definition) {
		def builder=new FixedLenFileBuilder()
		def p=builder.definition
		def f=new File(filename)
		if (f.exists()) {
			f.eachLine { line->
				p.parse(line,initialContext)
			}
		} else {
			throw new FileNotFoundException("File ${filename} not found")
		}
	}
	
	def generate(String filename, data, initialContext=[:], Closure definition) {
		def builder=new FixedLenFileBuilder()
		def fmt=builder.definition
		def f=new File(filename)
		if (data && data instanceof List) {
			f.withPrintWriter { out->
				data.each { reg->  
					def line=fmt.generate(reg,'')
					out.print(line)	
				}
			}
		} else throw FixedLenFileException("Invalid data, expected a list of maps or objects")
	}
}

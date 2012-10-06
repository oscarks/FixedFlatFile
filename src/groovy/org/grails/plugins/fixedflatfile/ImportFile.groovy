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

class ImportFile {
    def skip=0
    def cls=null
	def exitOnError=false
	def formatadores=[:]
	def evaluated=[:]
	def ids=[]
	
	String capitalize(String s) {
		char[] chars = s.toCharArray()
		chars[0] = Character.toUpperCase(chars[0])
		return new String(chars)
	}
	def loadToDomain(config,filename, domainClass) {
		cls=domainClass
		execute(config,filename,saveObject)
	}
	def fileToDomain(config,f, domainClass) {
		cls=domainClass
		executeInFile(config,f,saveObject)
	}
	def fileToDomain(File file, inputFile, domainClass) {
		cls=domainClass
		executeFileWithRecords(file, inputFile)
	}
    def execute(config,filename,clos){
        def f=new java.io.File(filename)
		executeInFile(config,f,clos)
    }
	
	def executeFileWithRecords(file, inputFile) {
		def records = RecordFile.withCriteria() {
			eq('file', file)
			order('ordem', 'asc')
		}
		
		def cfg
		def currentRecord
		def result=[errors:[], changes:[]]
		
		ClassLoader cl = getClass().getClassLoader()
		GroovyShell shell = new GroovyShell(cl)
		if(records) {
			inputFile.eachLine { line,i ->
				exitOnError=false
				try {
					def record=records.find {line==~/${it.pattern}/}
					if(record) {
						if(record!=currentRecord) {
							cfg=new XmlSlurper().parseText(record.config)
							init(cfg)
							exitOnError=record.critical
							currentRecord=record
						}
						def pars=process(cfg, line)
						pars.ids=ids
						pars.clazz=cls
						pars.inputFile=inputFile
						pars.instance=Util.loadObject(cls, ids, pars)
						// cria uma closure dinamicamente
						def eval=shell.evaluate("{ it -> ${record.eval}}")
						def changes=eval(pars)
						changes.each {
							it.linha=i
							it.texto=line
							result.changes<<it
						}
					} else if(i==1) {
						exitOnError=true
						throw new RuntimeException('O conteudo do Header foge ao padrao da especificacao do layout!')
					} else throw new RuntimeException("O conteudo da linha ${i} foge ao padrao da especificacao do layout!")
				}catch(Exception e) {
					if (exitOnError) {
						e.printStackTrace()
						throw e
					} else {
						def err=[linha:i, texto:line, erro:e.getMessage()]
						result.errors<<err
						println err
					}
				}
			}
		}
		
		result
	}
	
	def executeInFile(config,f,clos){
		def result=[errors:[], changes:[]]
		def cfg=new XmlSlurper().parseText(config)
		init(cfg)
		f.eachLine { line,i ->
			if (skip<=0){
				try {
					if (line) {
						def params=process(cfg,line)
						clos(params)
					}
				} catch (Exception e){
					if (exitOnError) {
						e.printStackTrace()
						throw e
					} else {
						def err=[linha:i, texto:line,erro:e.getMessage()]
						result.errors<<err
						println err
					}
				}
			} else skip--
		}
		result
	}
	
	def init = {cfg ->
		formatadores=[:]
		evaluated=[:]
		ids=[]
		cfg.property.each{ p ->
			if (p.format.size()>0) {
				def name=p.@name.text()
				def s=p.format.text()
				ClassLoader cl = getClass().getClassLoader();
				GroovyShell shell = new GroovyShell(cl);
				def m=shell.evaluate("{ it -> ${s}}")
				formatadores[name]=m
			} 
			if (p.@type.text() == 'evaluated') {
				def name=p.@name.text()
				def value=p.@value.text()
				ClassLoader cl = getClass().getClassLoader();
				GroovyShell shell = new GroovyShell(cl);
				value=shell.evaluate(value)
				evaluated[name]=value
			} 
			if(p.@type.text() == 'id') {
				ids << p.@name.text()
			}
		}
	}
	
    def process(cfg, line){
        def p=[:]
        int currentPosition = 0
        cfg.property.each {
		if (it.@type.text() in ['id','data','control']) {
		        int begin = currentPosition
		        int end = currentPosition+(it.@size.text() as int) - 1
		        currentPosition += it.@size.text() as int
		        def value = line[begin..end]
		        if(it.@type.text() != 'control') {
				def name=it.@name.text()
				if (formatadores[name]) {
					def format=formatadores[name]
					value=format(value.trim())
				}  else value=value.trim()
		            	p[name]=value
		        }           
        	} else if (it.@type.text() == 'fixo') {
				def name=it.@name.text()
				def value=it.@value.text()
				p[name]=value	
			} else if (it.@type.text() == 'evaluated') {
				def name=it.@name.text()
				p[name]=evaluated[name]	
			}
        }
        
    }
	
	def loadObject = {ids,pars ->
		def d
		def findFunc='findBy'
		if (ids) {
			for(def id in ids) {
				if (findFunc=='findBy') findFunc=findFunc+capitalize(id)
				else findFunc=findFunc+'And'+capitalize(id)
			}
			def findPars=ids.collect { pars[it] }
			if (findPars.size()==1) d=cls."${findFunc}"(findPars[0])
			else d=cls."${findFunc}"(findPars)
		}
		if (!d) d=cls.newInstance()
		d
	}
	
	def saveObject = {pars ->
		def d = loadObject(ids, pars)
		d.properties=pars
		if (!d.save()) {
			def err=""
			d.errors.allErrors.each { e->
				err += e
			}
			throw new RuntimeException('Erro ao processar registro:'+err)
		}
	}
	
}

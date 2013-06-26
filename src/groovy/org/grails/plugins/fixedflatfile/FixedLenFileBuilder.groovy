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

import groovy.util.BuilderSupport
/**
 * Builder
 * Build tree of parse and format definition for the fixed len field flat file.
 * @author Oscar Konno Sampaio  (oscarks@gmail.com)
 *
 */
class FixedLenFileBuilder extends BuilderSupport {
    int position=1
	def stack=[]
    def steps
	
	def enqueue() {
		stack.add(position)
		position
	}
	
	def dequeue() {
		if (stack.size()==1) {
			position=stack[0]
			stack=[]
		} else if (stack.size()>1) {
			position=stack[-1]
			stack=stack[0..-2]
		}
		return position
	}
	
    protected void setParent(Object parent, Object child){
        if(parent instanceof Steps && child instanceof Command) {
            parent.commands.add(child)
        } else if (parent instanceof ControlCommand && child instanceof Steps) {
            parent.steps=child
        } else if (parent instanceof ControlCommand && child instanceof Command) {
            if (!parent.steps) {
                parent.steps=new Steps()
                parent.steps.commands=[]
            }
            parent.steps.commands.add(child)
        } else throw new FixedLenFileException("Invalid relationship ${parent} -> ${child}")
    }

    protected Object createNode(Object name, Map attributes){
        if(name=='steps') {
            def s=new Steps()
            def n=attributes['name']
            def closure=attributes['closure']
			def format=attributes['format']
            s.commands=[]
            s.name=n
            s.closure=closure
			if (format) s.format=format
            return s;
        } else if(name=='if') {
            def test=attributes['test']
			def closure=attributes['closure']
			enqueue()
            return new IfCommand(test:test,closure:closure)
            
        } else if(name=='eq') {
			def closure=attributes['closure']
			def pars=attributes.findAll {k,y -> k!='closure' }
            def e=new EqualCommand(parameters:pars,closure:closure)
			enqueue()
            return e
        } else if (name=='se') {
			def closure=attributes['closure']
			def s=attributes['size']
			def e=new SizeEqualCommand(size:s,closure:closure)
			enqueue()
			return e
		} else if(name=='TAB') {
			position=position+1
			return new Tab()
		} else if(name=='NL') {
			def type=attributes['type']
			def len=1
			if (type) {
				type=type.toLowerCase()
				if (type in ['windows','dos','os2','symbian','palm']) {
					type='dos'
					len=2
				} else if (type in ['linux','unix']) type='linux'
				else throw new FixedLenFileException("Invalid new line type (${type}). Expected types: 'windows','dos','os2','symbian','palm','linux','unix'")
			} else type='linux'
			position=position+len
			return new NewLine(type:type)
        } else {
            def begin=attributes['begin']
            def end=attributes['end']
            def size=attributes['size']
            def closure=attributes['closure']
            def convert=attributes['convert']
			def format=attributes['format']
			def notrim=attributes['notrim']
			def truncate=attributes['truncate']
			
            if (!begin) begin=position
            if (end) {
                if (end<begin) throw new Exception("Fim antes do inicio")
                size=end-begin+1
            } else if (size) {
                end=begin+size-1
            } else {
                throw new FixedLenFileException("Falta definir o fim ou tamanho do campo ${name}")
            }
            def n=attributes['name']
            def f=new Field(begin:begin,end:end,name:name,closure:closure,notrim:notrim,convert:convert,format:format)
			if(truncate!=null) f.truncate=truncate
			
            position=end+1
            return f
        }
    }

    protected Object createNode(Object name){
		if (name=='NL') {
			def e=new NewLine()
			position=position+1
			return e
		} else if (name=='TAB') {
			def e=new Tab()
			position=position+1
			return e
		} else {
	        def s=new Steps()
	        s.commands=[]
	        s.name=name
	        return s;
		}
    }
    protected Object createNode(Object name, Object value){
        if(name=='steps') {
            def s=new Steps()
            s.command=[]
            s.name=value
            return s;
        } else if (name=='se') {
			def s=value.toInteger()
			def e=new SizeEqualCommand(size:s,closure:closure)
			enqueue()
			return e
		} else if (name=='constant') {
			def e=new Constant(value:value)
			position=position+value.toString().size()
			return e
		} else if (name=='filler') {
			def e=new Filler(length:value.toInteger())
			position=position+value.toInteger()
			return e
		} else {
            def begin=position
            def size=value.toInteger()
            def end=begin+size-1
            def f=new Field(begin:begin,end:end,name:name)
            position=end+1
            return f
        }
    }
    protected Object createNode(Object name, Map attributes, Object value){
        if(name=='steps') {
            def s=new Steps()
            def n=attributes['name']
            s.command=[]
            s.name=n
            if (value instanceof Closure)
                s.closure=value
            return s;
        } else if(name=='eq') {
			def closure=attributes['closure']
			def pars=attributes.findAll {k,y -> k!='closure' }
            def e=new EqualCommand(parameters:pars,closure:closure)
            return e
        } else if(name=='se') {
			def closure=attributes['closure']
			def s=value.toInteger()
			def e=new SizeEqualCommand(size:s,closure:closure)
			enqueue()
			return e
        } else if(name=='if') {
            def test=attributes['test']
			def closure=attributes['closure']
            return new IfCommand(test:test,closure:closure)
        } else if(name=='constant') {
			def align=attributes['align']
			def length=attributes['length']
			if (!length) length=value.toString().size()
			def c=new Constant(value:value)
			if (align) c.align=align
			if (length) c.length=length
			position=position+length
			return c
        } else if(name=='filler') {
			def filler=attributes['char']
			def f=new Filler(length:value.toInteger())
			if (filler) f.filler=filler
			position=position+value.toInteger()
			return f
        } else if(name=='TAB') {
			position=position+1
			return new Tab()
        } else if(name=='NL') {
			def type=attributes['type']
			def len=1
			if (type) {
				type=type.toLowerCase()
				if (type in ['windows','dos','os2','symbian','palm']) {
					type='dos'
					len=2
				} else if (type in ['linux','unix']) type='linux'
				else throw new FixedLenFileException("Invalid new line type (${type}). Expected types: 'windows','dos','os2','symbian','palm','linux','unix'")
			} else type='linux'
			position=position+len
			return new NewLine(type:type)
    	} else {
            def begin=attributes['begin']
            def end=attributes['end']
            def size=attributes['size']
            def closure=attributes['closure']
			def convert=attributes['convert']
			def format=attributes['format']
			def notrim=attributes['notrim']
			def align=attributes['align']
			def defaultValue=attributes['default']
			if (align) {
				align=align.toUpperCase()
				if (align in ['R','RIGHT','>']) align='R'
				else if (align in ['L','LEFT','<']) align='L'
				else throw new FixedLenFileException("Invalid align (${align}) expected values: ['R','RIGHT','L','LEFT']")
			}
			def type=attributes['type']
			if (type) {
				type=type.toUpperCase()
				if (type in ['A','ALPHA','ALPHANUMERIC']) type='A'
				else if (type in ['N','NUMERIC']) type='N'
				else throw new Exception("Invalid type (${type}) expected values: ['A','ALPHA','N','NUMERIC']")
			}
			
            if (!begin) begin=position
            if (end) {
                if (end<begin) throw new FixedLenFileException("End of field defined before begin")
                size=end-begin+1
            } else if (size) {
                end=begin+size-1
            } else if(value instanceof Integer){
                size=value
                end=begin+size-1
            } else {
                throw new FixedLenFileException("Undefined field size, please define size or begin and and parameters")
            }
            def f=new Field(begin:begin,end:end,name:name,closure:closure,convert:convert,notrim:notrim,format:format)
			if (type) f.type=type
			if (align) f.align=align
			if (defaultValue!=null) f.defaultValue=defaultValue
            position=end+1
            return f
        }
    }
    protected void nodeCompleted(Object parent, Object node) {
		if (node instanceof ControlCommand) {
			dequeue()
		}
    }
}

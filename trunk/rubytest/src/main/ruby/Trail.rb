require 'java'

module Org
  module Osgiscripting
    module Rubytest
      # Instrumented test implementation of a backup service in JRuby. Used to test
      # that core module works with JRuby scripts for plugin modules.
      class ScriptServiceImpl 
        include org.osgiscripting.apitest.ScriptService;
      
        def run(arg) 
          return "jruby called with value " + arg
        end
      end # class
    end
  end  
end

return Org::Osgiscripting::Rubytest::ScriptServiceImpl.new
importPackage(org.osgiscripting.apitest)

function ScriptServiceImpl() {
	return {
		run: function(arg) {
			return 'javascript called with value ' + arg
  		}
	}
}

ScriptServiceImpl()
var exec = require('cordova/exec');
var SerialPortRW = function() {};

SerialPortRW.prototype.get = function(success, error) {
	exec(success, error, "SerialPortRW", "get", []);
};

SerialPortRW.prototype.open = function(port, baudrate, success, error) {
	exec(success, error, "SerialPortRW", "open", [port, baudrate]);
};

SerialPortRW.prototype.close = function(port,success, error) {
	exec(success, error, "SerialPortRW", "close", [port]);
};

SerialPortRW.prototype.emission = function(port,byteArr, success, error) {
	exec(success, error, "SerialPortRW", "emission", [port,byteArr]);
};

SerialPortRW.prototype.detect = function(success, error) {
	exec(success, error, "SerialPortRW", "detect", []);
};

SerialPortRW.prototype.listen=function(port,success,error){
	exec(success, error, "SerialPortRW", "listen", [port]);
};

module.exports = new SerialPortRW();
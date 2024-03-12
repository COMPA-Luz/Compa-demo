<?php
require_once '../simplesamlphp/lib/SimpleSAML/Session.php';

// 从命令行参数获取$obj和$str
$objSerialized = $argv[1] ?? '';
$str = $argv[2] ?? '';



// 实例化MyClass并调用isValid方法
$mySessionInstance = new Session($objSerialized);
$result = $mySessionInstance->isValid($str);

// 输出结果
echo $result ? "true" : "false";

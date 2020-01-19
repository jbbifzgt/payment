<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<body>
		<div>支付产品通用接口测试</div>
		<div id="input">
		<form method="post" action="pay/commit">
			<div>商家订单号:<input type="text" name="orderId" /></div>
			<div>支付通道编码:<input type="text" name="channel"  /></div>
			<div><input type="submit" value="结帐" /></div>
			</form>
		</div>
	</body>
</body>
</html>
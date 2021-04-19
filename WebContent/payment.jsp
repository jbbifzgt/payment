<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<script type="text/javascript" src="${webcontext }/jquery.min.js"></script>
<script type="text/javascript">
	$(function(){
		var timer = null;
		function check(){
			
			var orderId =$('#orderId').val(); 
			$.get('${webcontext}/payment/pay/status',{orderId:orderId},function(json){
				console.log(json);
				if(json.status == 2){
					$(document.body).append('支付成功');
					clearTimeout(timer);
				}else{
					window.clearTimeout(timer);
					timer = setTimeout(check, 500);
				}
			},'json');
		}
		
		$('#frm001').submit(function(){
			$(document.body).append('开始支付');
			check();
		});
	});
</script>
</head>
<body>
<body>
		<div>支付产品通用接口测试</div>
		<div id="input">
		<form method="post" id="frm001" action="payment/pay/commit" target="_blank">
			<div>支付方式：<input type="radio" name="type" value="alipay"> 支付宝
			 <input type="radio" name="type" value="tenpay"> 财富通 
			 <input type="radio" name="type" value="99bill"> 快钱
			 
			  </div>
			<div>商家订单号:<input type="text" name="orderId" id="orderId" /></div>
			<div>支付通道编码:<input type="text" name="channel"  /></div>
			<div><input type="submit" value="结帐" /></div>
			</form>
		</div>
	</body>
</body>
</html>
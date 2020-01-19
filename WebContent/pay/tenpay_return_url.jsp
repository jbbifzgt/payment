<%@page import="lion.dev.lang.MapJ"%>
<%@page import="lion.payment.port.tenpay.TenpayPort"%>
<%@page import="lion.payment.PaymentConfig"%>
<%@ page language="java" contentType="text/html; charset=GBK" pageEncoding="GBK" trimDirectiveWhitespaces="true"%>
<%
	MapJ portConfig = PaymentConfig.getPortConfig(TenpayPort.PORT_NAME);
%>
<meta name="TENCENT_ONELINE_PAYMENT" content="China TENCENT">
<html>
<script language=javascript>
	window.location.href='<%=portConfig.getString("returnURL")%>';
</script>
</html>
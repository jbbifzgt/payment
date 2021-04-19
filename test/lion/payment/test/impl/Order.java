package lion.payment.test.impl;

import lion.payment.IPaymentOrder;

public class Order implements IPaymentOrder {

	private String id;
	private double amount;
	private String productId;
	private String orderName;
	private String orderDescribtion;
	private String orderCategory;
	private int status;
	private String port;

	@Override
	public String getId() {

		return id;
	}

	public void setId(String id) {

		this.id = id;
	}

	@Override
	public double getAmount() {

		return amount;
	}

	public void setAmount(double amount) {

		this.amount = amount;
	}

	public String getProductId() {

		return productId;
	}

	public void setProductId(String productId) {

		this.productId = productId;
	}

	@Override
	public String getOrderName() {

		return orderName;
	}

	public void setOrderName(String orderName) {

		this.orderName = orderName;
	}

	public String getOrderDescribtion() {

		return orderDescribtion;
	}

	public void setOrderDescribtion(String orderDescribtion) {

		this.orderDescribtion = orderDescribtion;
	}

	public String getOrderCategory() {

		return orderCategory;
	}

	public void setOrderCategory(String orderCategory) {

		this.orderCategory = orderCategory;
	}

	@Override
	public String getPortName() {

		return this.port;
	}

	public int getStatus() {

		return status;
	}

	public void setStatus(int status) {

		this.status = status;
	}

	public String getPort() {

		return port;
	}

	public void setPort(String port) {

		this.port = port;
	}

}

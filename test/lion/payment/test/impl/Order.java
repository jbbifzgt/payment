package lion.payment.test.impl;

import java.util.Date;

import lion.payment.IPaymentOrder;

public class Order implements IPaymentOrder {

	private String id;
	private double amount;
	private String productId;
	private String orderName;
	private String orderDescribtion;
	private String orderCategory;

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

	@Override
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

	@Override
	public String getOrderDescribtion() {

		return orderDescribtion;
	}

	public void setOrderDescribtion(String orderDescribtion) {

		this.orderDescribtion = orderDescribtion;
	}

	@Override
	public String getOrderCategory() {

		return orderCategory;
	}

	public void setOrderCategory(String orderCategory) {

		this.orderCategory = orderCategory;
	}

	@Override
	public String getTransId() {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getTransDate() {

		// TODO Auto-generated method stub
		return null;
	}

}

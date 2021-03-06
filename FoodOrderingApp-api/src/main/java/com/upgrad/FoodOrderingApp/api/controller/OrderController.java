package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.model.*;
import com.upgrad.FoodOrderingApp.service.businness.*;
import com.upgrad.FoodOrderingApp.service.entity.*;
import com.upgrad.FoodOrderingApp.service.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    CustomerService customerService;

    @Autowired
    AddressService addressService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    ItemService itemService;

    @Autowired
    RestaurantService restaurantService;



    @CrossOrigin
    @GetMapping(path = "/order/coupon/{coupon_name}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CouponDetailsResponse> getCouponByName(@RequestHeader("authorization")
                                                                 final String access_token,
                                                                 @PathVariable final String coupon_name)
            throws AuthorizationFailedException, CouponNotFoundException {
        CustomerEntity customerEntity = customerService.getCustomer(access_token);

        if(String.valueOf(coupon_name).equals("null") || coupon_name.isEmpty()){
            throw new CouponNotFoundException("CPF-002",
                    "Coupon name field should not be empty");
        }

        CouponEntity couponEntity = orderService.getCouponByCouponName(coupon_name);

        CouponDetailsResponse couponDetailsResponse = new CouponDetailsResponse();
        String uuid = couponEntity.getUuid();

        couponDetailsResponse.setCouponName(couponEntity.getCoupon_name());
        couponDetailsResponse.setId(UUID.fromString(uuid));
        couponDetailsResponse.setPercent(couponEntity.getPercent());

        return new ResponseEntity<CouponDetailsResponse>(couponDetailsResponse, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(path = "/order", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SaveOrderResponse> saveOrder(@RequestHeader final String access_token,
                                                       final SaveOrderRequest saveOrderRequest)
            throws AuthorizationFailedException, CouponNotFoundException, AddressNotFoundException,
            PaymentMethodNotFoundException, RestaurantNotFoundException, ItemNotFoundException {
        // Authorize (also handles authorization related exceptions)
        CustomerEntity customerEntity = customerService.getCustomer(access_token);

        // Handle coupon related exceptions
        CouponEntity couponEntity = orderService.getCouponByCouponId(saveOrderRequest.getCouponId().toString());

        // Handle address related exceptions
        AddressEntity addressEntity = addressService.getAddressByUUID(saveOrderRequest.getAddressId().toString(),
                customerEntity);

        // Handle payment related exceptions
        PaymentEntity paymentEntity = paymentService.getPaymentByUUID(saveOrderRequest.getPaymentId().toString());

        // Handle restaurant exception
        RestaurantEntity restaurantEntity = restaurantService.getRestaurantByUuid(saveOrderRequest.getRestaurantId().toString());
        // Handle item exception

        List<ItemQuantity> itemQuantities = saveOrderRequest.getItemQuantities();
        for (ItemQuantity iq : itemQuantities) {

            ItemEntity itemEntity = itemService.getItemByUuid(iq.getItemId());

        }


        // Create OrderEntity to persist
        OrderEntity orderEntity = new OrderEntity();

        // Add details
        orderEntity.setUuid(UUID.randomUUID().toString());
        orderEntity.setAddressEntity(addressEntity);
        orderEntity.setBill(saveOrderRequest.getBill());
        orderEntity.setCouponEntity(couponEntity);
        orderEntity.setCustomerEntity(customerEntity);
        orderEntity.setPaymentEntity(paymentEntity);
        orderEntity.setDiscount(saveOrderRequest.getDiscount());
        orderEntity.setDate(ZonedDateTime.now());
        // set restaurant id here
        orderEntity.setRestaurant_id(restaurantEntity.getId());

        //persist order
        orderService.saveOrder(orderEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // Add order item details
        for (ItemQuantity iq : itemQuantities) {
            ItemEntity itemEntity = itemService.getItemByUuid(iq.getItemId());
            orderItemEntity.setOrder(orderEntity);
            orderItemEntity.setPrice(iq.getPrice());
            orderItemEntity.setItem(itemEntity);
            orderItemEntity.setQuantity(iq.getQuantity());
            orderItemService.saveOrder(orderItemEntity);
        }


        // Return response
        SaveOrderResponse saveOrderResponse = new SaveOrderResponse();

        saveOrderResponse.setId(UUID.randomUUID().toString());
        saveOrderResponse.setStatus("ORDER SUCCESSFULLY PLACED");

        return new ResponseEntity<SaveOrderResponse>(saveOrderResponse, HttpStatus.OK);
    }


    @CrossOrigin
    @GetMapping(path = "/order", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CustomerOrderResponse> getOrdersByCustomer(@RequestHeader("authorization") final String access_token)
            throws AuthorizationFailedException, ItemNotFoundException {
        CustomerEntity customerEntity = customerService.getCustomer(access_token);

        List<OrderEntity> orderEntityList = orderService.getOrdersByCustomer(customerEntity.getId());

        CustomerOrderResponse customerOrderResponse = new CustomerOrderResponse();

        if (orderEntityList.size() > 0) {
            for (int i = 0; i < orderEntityList.size(); i++) {
                OrderList orderList = new OrderList();
                String customerUUID = orderEntityList.get(i).getCustomerEntity().getUuid();
                String orderUUID = orderEntityList.get(i).getUuid();
                String couponUUID = orderEntityList.get(i).getCouponEntity().getUuid();
                String addressUUID = orderEntityList.get(i).getAddressEntity().getUuid();
                String stateUUID = orderEntityList.get(i).getAddressEntity().getStateEntity().getUuid();
                OrderListAddressState orderListAddressState = new OrderListAddressState();

                // Order list address state
                orderListAddressState.setId(UUID.fromString(stateUUID));
                orderListAddressState.setStateName(orderEntityList.get(i).getAddressEntity().getStateEntity().getState_name());

                // Order list Address
                OrderListAddress orderListAddress = new OrderListAddress();
                orderListAddress.setCity(orderEntityList.get(i).getAddressEntity().getCity());
                orderListAddress.setFlatBuildingName(orderEntityList.get(i).getAddressEntity().getFlat_buil_number());
                orderListAddress.setId(UUID.fromString(addressUUID));
                orderListAddress.setLocality(orderEntityList.get(i).getAddressEntity().getLocality());
                orderListAddress.setPincode(orderEntityList.get(i).getAddressEntity().getPincode());
                orderListAddress.setState(orderListAddressState);

                // Order List Coupon
                OrderListCoupon orderListCoupon = new OrderListCoupon();
                orderListCoupon.setCouponName(orderEntityList.get(i).getCouponEntity().getCoupon_name());
                orderListCoupon.setId(UUID.fromString(couponUUID));
                orderListCoupon.setPercent(orderEntityList.get(i).getCouponEntity().getPercent());

                // Order List Customer
                OrderListCustomer orderListCustomer = new OrderListCustomer();
                orderListCustomer.setContactNumber(customerEntity.getContact_number());
                orderListCustomer.setEmailAddress(customerEntity.getEmail());
                orderListCustomer.setFirstName(customerEntity.getFirstName());
                orderListCustomer.setId(UUID.fromString(customerUUID));

                // Order List Payment
                OrderListPayment orderListPayment = new OrderListPayment();
                orderListPayment.setId(UUID.fromString(orderEntityList.get(i).getUuid()));
                orderListPayment.setPaymentName(orderEntityList.get(i).getPaymentEntity().getPayment_name());

                // Item quantity
                List<ItemQuantityResponse> itemQuantityResponseList = new ArrayList<>();
                List<OrderItemEntity> orderItemEntities = orderItemService.getOrderItemEntityByOrderId(orderEntityList.get(i).getId());
                for (OrderItemEntity or : orderItemEntities) {

                    ItemEntity itemEntity = itemService.getItemById(or.getItem());
                    ItemQuantityResponseItem itemQuantityResponseItem = new ItemQuantityResponseItem()
                            .id(UUID.fromString(itemEntity.getUuid())).itemPrice(itemEntity.getPrice())
                            .itemName(itemEntity.getItemName())
                            .type(ItemQuantityResponseItem.TypeEnum.fromValue(itemEntity.getType()));
                    ItemQuantityResponse itemQuantityResponse = new ItemQuantityResponse()
                            .item(itemQuantityResponseItem).quantity(or.getQuantity()).price(or.getPrice());
                    itemQuantityResponseList.add(itemQuantityResponse);
                }



                orderList.setAddress(orderListAddress);
                orderList.setBill(orderEntityList.get(i).getBill());
                orderList.setDate(orderEntityList.get(i).getDate().toString());
                orderList.setDiscount(orderEntityList.get(i).getDiscount());
                orderList.setId(UUID.fromString(orderUUID));
                orderList.setCoupon(orderListCoupon);
                orderList.setCustomer(orderListCustomer);
                orderList.setPayment(orderListPayment);
                orderList.setItemQuantities(itemQuantityResponseList);

                customerOrderResponse.addOrdersItem(orderList);
            }
        } else {
            customerOrderResponse.setOrders(Collections.emptyList());
        }

        return new ResponseEntity<CustomerOrderResponse>(customerOrderResponse, HttpStatus.OK);
    }
}

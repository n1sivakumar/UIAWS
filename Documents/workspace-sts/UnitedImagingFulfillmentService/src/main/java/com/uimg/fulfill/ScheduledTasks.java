package com.uimg.fulfill;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.amazonservices.mws.client.MwsUtl;
import com.amazonservices.mws.orders._2013_09_01.MarketplaceWebServiceOrdersClient;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrderItemsResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersByNextTokenRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersByNextTokenResponse;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersRequest;
import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersResponse;

@Configuration
@PropertySource("classpath:config.properties")
@Component

public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    @Autowired
    private Environment env;
    
    @Scheduled(fixedRate = 30*60*1000)
    public void getOrders() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerException {
        log.info("The time is now {}", dateFormat.format(new Date()));
        // Get a client connection.
        // Make sure you've set the variables in UImgConfig.
        MarketplaceWebServiceOrdersClient client = UImgConfig.getClient();

        // Create a request.
        ListOrdersRequest lORequest = new ListOrdersRequest();
        String sellerId = env.getProperty("config.SellerId");
        lORequest.setSellerId(sellerId);
        String mwsAuthToken = "";
        lORequest.setMWSAuthToken(mwsAuthToken);
        GregorianCalendar c = new GregorianCalendar();
//        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        Date date = format.parse("2018-08-01 00:15:00");
        Date date = new Date();
        c.setTime(date);
        Integer intervalMins = Integer.parseInt(env.getProperty("config.IntervalMins"));
        c.add(Calendar.MINUTE, 0 - intervalMins); // From last interval minutes
        String outText = "Fetching Orders Created After: " + c.getTime();
        log.info(outText);
        System.out.println(outText);
        XMLGregorianCalendar createdAfter = MwsUtl.getDTF().newXMLGregorianCalendar(c);
        lORequest.setCreatedAfter(createdAfter);
//        XMLGregorianCalendar createdBefore = MwsUtl.getDTF().newXMLGregorianCalendar(c);
//        request.setCreatedBefore(createdBefore);
//        XMLGregorianCalendar lastUpdatedAfter = MwsUtl.getDTF().newXMLGregorianCalendar(c);
//        request.setLastUpdatedAfter(lastUpdatedAfter);
//        XMLGregorianCalendar lastUpdatedBefore = MwsUtl.getDTF().newXMLGregorianCalendar();
//        request.setLastUpdatedBefore(lastUpdatedBefore);
        List<String> orderStatus = new ArrayList<String>();
        lORequest.setOrderStatus(orderStatus);
        List<String> marketplaceId = new ArrayList<String>(Arrays.asList(env.getProperty("config.MarketplaceId")));
        lORequest.setMarketplaceId(marketplaceId);
        List<String> fulfillmentChannel = new ArrayList<String>();
        lORequest.setFulfillmentChannel(fulfillmentChannel);
        List<String> paymentMethod = new ArrayList<String>();
        lORequest.setPaymentMethod(paymentMethod);
//        String buyerEmail = "example";
//        request.setBuyerEmail(buyerEmail);
        String sellerOrderId = "";
        lORequest.setSellerOrderId(sellerOrderId);
        Integer maxResultsPerPage = 1;
        Integer orderCount = 0;
        lORequest.setMaxResultsPerPage(maxResultsPerPage);
        List<String> tfmShipmentStatus = new ArrayList<String>();
        lORequest.setTFMShipmentStatus(tfmShipmentStatus);

        // Make the call.
        //System.out.println(lOequest.getCreatedBefore());
        ListOrdersResponse response = UImgUtil.invokeListOrders(client, lORequest);
        String responseXml = response.toXML();
        //System.out.println(responseXml);
		String orderText = UImgUtil.getSubTreeFromXml(responseXml, "//Order").trim();
		
        if (orderText == null || orderText.length() == 0) {
        	outText += "\n" + orderCount + " Orders Fetched";
        	log.info(outText);
        	return;
        }
        System.out.println("Order :" + orderText + ":");
        /*
         * do while no more tokens
         *   ListOrderItem(responseXml.AmazonOrderId)
         *   ListOrdersByNextToken(responseXml.NextToken)
         * end
         */
        String amazonOrderId = null;
        ListOrderItemsRequest lOIRequest = new ListOrderItemsRequest();
        ListOrdersByNextTokenRequest lOBNTRequest = new ListOrdersByNextTokenRequest();
        boolean done = false;
        do  {
            orderCount++;
			amazonOrderId = UImgUtil.getValueFromXml(responseXml, "//AmazonOrderId").trim();
            System.out.println("Order ID: " + amazonOrderId);
//            Files.write(Paths.get(env.getProperty("config.FTPPath")+amazonOrderId+"_Order.xml")
//            		, orderText.getBytes());
            // Get order item.
            lOIRequest.setSellerId(sellerId);
            lOIRequest.setMWSAuthToken(mwsAuthToken);
            //String amazonOrderId = "112-5733050-7199436";
            lOIRequest.setAmazonOrderId(amazonOrderId);

            // Make the call.
            ListOrderItemsResponse response2 = UImgUtil.invokeListOrderItems(client, lOIRequest);
    		String orderItemText = UImgUtil.getSubTreeFromXml(response2.toXML(), "//OrderItem").trim();
            System.out.println("Order Item : " + orderItemText);
//            Files.write(Paths.get(env.getProperty("config.FTPPath")+amazonOrderId+"_OrderItem.xml")
//            		, orderItemText.getBytes());

            String orderXmlFile = Paths.get(env.getProperty("config.FTPPath")+amazonOrderId+"_Orders.xml").toString();
            try (FileWriter fw = new FileWriter(orderXmlFile);
            	    BufferedWriter bw = new BufferedWriter(fw);
            	    PrintWriter out = new PrintWriter(bw))
            {
            	out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            	out.println("<AmazonOrder>");
            	out.println(orderText);
            	out.println(orderItemText);
            	out.println("</AmazonOrder>");
            	out.close();
            } catch (IOException e) {
            }

            // Create a request.
            lOBNTRequest.setSellerId(sellerId);
            lOBNTRequest.setMWSAuthToken(mwsAuthToken);
            String nextToken = null;
			nextToken = UImgUtil.getValueFromXml(responseXml, "//NextToken");
//            System.out.println("Next Token :" + nextToken + ":");
            if (nextToken == null || nextToken.length() == 0) {
            	done = true;
            } else {
	            lOBNTRequest.setNextToken(nextToken);
	
	            // Make the call.
	            ListOrdersByNextTokenResponse response3 = UImgUtil.invokeListOrdersByNextToken(client, lOBNTRequest);
				orderText = UImgUtil.getSubTreeFromXml(response3.toXML(), "//Order");
	            System.out.println("Order : " + orderText);
	            responseXml = response3.toXML();
            }
        } while ( !done );
    	outText += "\n" + orderCount + " Orders Fetched";
    	log.info(outText);
    	return;

    }
}

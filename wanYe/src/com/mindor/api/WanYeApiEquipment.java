package com.mindor.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import com.mindor.entity.ChargeGuard;
import com.mindor.entity.Equipment;
import com.mindor.entity.EquipmentInfo;
import com.mindor.entity.Intermediate;
import com.mindor.entity.MessageManage;
import com.mindor.entity.OpenUser;
import com.mindor.entity.Product;
import com.mindor.entity.Product_equipment;
import com.mindor.entity.ShareRecord;
import com.mindor.serivce.ChargeGuardService;
import com.mindor.serivce.EquipmentService;
import com.mindor.serivce.MessageManageService;
import com.mindor.serivce.PowerCountService;
import com.mindor.serivce.ProductService;
import com.mindor.serivce.TimeDelayService;
import com.mindor.serivce.TimingService;
import com.mindor.serivce.UserService;
import com.mindor.util.ClientMQTT;
import com.opensymphony.xwork2.ActionSupport;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAilabsIotDeviceListUpdateNotifyRequest;
import com.taobao.api.response.AlibabaAilabsIotDeviceListUpdateNotifyResponse;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.CycleDetectionStrategy;


@SuppressWarnings({ "serial"})
public class WanYeApiEquipment extends ActionSupport {

	private int code;

	public MessageManageService getMessageManageService() {
		return messageManageService;
	}

	public void setMessageManageService(
			MessageManageService messageManageService) {
		this.messageManageService = messageManageService;
	}

	private String MessageStr;
	OpenUser OpenUser = new OpenUser();
	Equipment equipment = new Equipment();
	private EquipmentService equipmentService;
	public PowerCountService getPowerCountService() {
		return powerCountService;
	}

	public void setPowerCountService(PowerCountService powerCountService) {
		this.powerCountService = powerCountService;
	}

	private PowerCountService powerCountService;

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	private ProductService productService;
	private TimingService timingService;
	private UserService userService;

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	private TimeDelayService timeDelayService;
	private ChargeGuardService chargeGuardService;
	private MessageManageService messageManageService;
	Logger logger = Logger.getLogger(WanYeApiEquipment.class);

	public ChargeGuardService getChargeGuardService() {
		return chargeGuardService;
	}

	public void setChargeGuardService(ChargeGuardService chargeGuardService) {
		this.chargeGuardService = chargeGuardService;
	}

	/**
	 * 设备列表
	 * 
	 * @throws IOException
	 * @throws IOException
	 */
	public void loadEquipment() throws IOException {

		// 提供了一个过滤作用，如果遇到关联的对象时他会自动过滤掉，不去执行关联关联所关联的对象。
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setExcludes(new String[] { "equipment", "openUser" });// 在这里添加要过滤的属性名
		jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);

		ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
		PrintWriter out;
		out = ServletActionContext.getResponse().getWriter();

		JSONObject json = new JSONObject();

		String sql = null;
		JSONArray jsonObject = null;

		String sql02 = null;

		String productId = ServletActionContext.getRequest().getParameter("productId");
		String userId = ServletActionContext.getRequest().getParameter("userId");
		String equipmentId = ServletActionContext.getRequest().getParameter("equipmentId");
		String way = ServletActionContext.getRequest().getParameter("way");

		System.out.println("productId==============" + productId);
		System.out.println("equipmentId==============" + equipmentId);
		
		if(userId!=null&&userId!="") {

		sql = "SELECT b.equipmentId,b.equipmentName,d.equipmentNote,d.equipmentLabel,d.specificEquipmentLabel,a.productId,a.productIcon,a.productImage,b.equipmentState,d.role,d.warnValue,d.blackout,d.warnOperate FROM product a,equipment b,openuser c,intermediate d WHERE  a.productId=d.productId AND b.equipmentId=d.equipmentId AND c.userId=d.userId AND c.userId='"
				+ userId + "' AND a.productId='" + productId + "' ";

		if (equipmentId != null) {
			String equipmentIdStr = equipmentId.substring(6, 7);
			StringBuffer sb = new StringBuffer(equipmentId);
			if (equipmentIdStr.equals("0")) {
				sb.replace(6, 7, "1");
				equipmentId = sb.toString();
			}
			System.out.println("处理后的设备id为：" + equipmentId);
			sql += " and b.equipmentId='" + equipmentId
					+ "' group by b.equipmentId";
		} else {
			sql += " group by b.equipmentId";
		}

		List<Object> list = equipmentService.selectEquipmentBySql(sql);

		System.out.println("list=============" + list);

		if (list.size() == 0) {// 未绑定产品
			sql = "SELECT a from Product a where a.productId='" + productId
					+ "' ";
			sql02 = "SELECT a from Equipment a where a.equipmentId='"
					+ equipmentId + "' ";

			List<Object> list02 = equipmentService.selectEquipmentByHql(sql);
			List<Object> list03 = equipmentService.selectEquipmentByHql(sql02);
			List<Object> list04;

			if (list03.size() == 0) { // 服务器未添加设备
				code = 500;
				MessageStr = "没有该设备！";
			} else {
				Product product = (Product) list02.get(0);
				Set<Equipment> equipmentList = product.getEquipment();
				Equipment equipment = (Equipment) list03.get(0);
				Iterator<Equipment> it = equipmentList.iterator();
				int i = 0;
				while (it.hasNext()) {
					Equipment objs = (Equipment) it.next();
					if (objs.getEquipmentId().equals(equipmentId)) {
						i = i + 1;
					}
				}
				if (i == 0) {
					code = 500;
					MessageStr = "设备不属于该产品！";
				} else {
					if (way == null || way == "") {
						sql = "SELECT a from Intermediate a where a.equipmentId='"
								+ equipmentId + "' and a.role='1'";
						list04 = equipmentService.selectEquipmentByHql(sql);
						if (list04.size() == 0) {
							list04 = null;
						}
					} else {
						list04 = null;
					}

					if (list04 == null) {
						Product_equipment product_equipment = new Product_equipment();
						if (way != null && way != "") { // 推送入口直接添加数据
							Intermediate intermediate = new Intermediate();
							intermediate.setUserId(userId);
							intermediate.setProductId(productId);
							intermediate.setEquipmentId(equipmentId);
							intermediate.setEquipmentLabel("客厅,主卧,餐厅,厨房,次卧");
							intermediate.setSpecificEquipmentLabel("客厅");
							intermediate.setEquipmentNote(equipment
									.getEquipmentName());
							intermediate.setRole("2");
							equipmentService.saveIntermediate(intermediate);

						}
						product_equipment.setProductId(product.getProductId());
						product_equipment.setProductName(product
								.getProductName());
						product_equipment.setProductImage(product
								.getProductImage());
						product_equipment.setEquipmentId(equipment
								.getEquipmentId());
						product_equipment.setEquipmentName(equipment
								.getEquipmentName());
						jsonObject = JSONArray.fromObject(product_equipment,
								jsonConfig);// 转json
						code = 200;
						MessageStr = "success";

					} else {
						Intermediate intermediate = new Intermediate();
						OpenUser openUser = new OpenUser();
						intermediate = (Intermediate) list04.get(0);
						System.out.println("intermediate.getUserId()"
								+ intermediate.getUserId());
						openUser = userService.selectNamebyId(intermediate
								.getUserId());
						code = 500;
						if (openUser.getNickName() != null
								&& openUser.getNickName().toString() != "") {
							MessageStr = "该设备已被  " + openUser.getNickName()
									+ " 绑定！是否发送授权申请?";
						} else if (openUser.getPhone() != null
								&& openUser.getPhone() != "") {
							MessageStr = "该设备已被 " + openUser.getPhone()
									+ " 绑定！是否发送授权申请?";
						} else {
							MessageStr = "该设备已被  " + openUser.getUserId()
									+ " 绑定！是否 发送授权申请?";
						}
					}
				}
			}

		} else {

			if (way == null || way == "") {// 非推送进入
				// 已绑定产品
				EquipmentInfo equipmentInfo = null;// 创建实体类
				List<EquipmentInfo> mylist = new LinkedList<EquipmentInfo>();

				Iterator<Object> it = list.iterator();
				while (it.hasNext()) {
					equipmentInfo = new EquipmentInfo();
					Object[] objs = (Object[]) it.next();
					equipmentInfo.setEquipmentId(String.valueOf(objs[0]));
					equipmentInfo.setEquipmentName(String.valueOf(objs[1]));
					equipmentInfo.setEquipmentNote(String.valueOf(objs[2]));
					equipmentInfo.setEquipmentLabel(String.valueOf(objs[3]));
					equipmentInfo.setSpecificEquipmentLabel(String
							.valueOf(objs[4]));
					equipmentInfo.setProductId(String.valueOf(objs[5]));
					equipmentInfo.setProductIcon(String.valueOf(objs[6]));
					equipmentInfo.setProductImage(String.valueOf(objs[7]));
					equipmentInfo.setEquipmentState(String.valueOf(objs[8]));
					equipmentInfo.setRole(String.valueOf(objs[9]));
					if (objs[10] == "" || objs[10] == null) {
						equipmentInfo.setWarnValue("30");
					} else {
						equipmentInfo.setWarnValue(String.valueOf(objs[10]));
					}

					if (objs[11] == "" || objs[11] == null) {
						equipmentInfo.setBlackout("15");
					} else {
						equipmentInfo.setBlackout(String.valueOf(objs[11]));
					}

					System.out.println("WarnValue==="
							+ String.valueOf(objs[12]));
					if (objs[12] == "" || objs[12] == null) {
						equipmentInfo.setWarnOperate("0");
					} else {
						equipmentInfo.setWarnOperate(String.valueOf(objs[12]));
					}
					mylist.add(equipmentInfo);
				}

				jsonObject = JSONArray.fromObject(mylist, jsonConfig);// 转json
				if (productId == null) {
					code = 500;
					MessageStr = "failure";
				} else {
					code = 200;
					MessageStr = "success";
				}
			} else {
				code = 500;
				MessageStr = "您已绑定了该设备！";
			}

		}
		json.put("code", code);
		json.put("Message", MessageStr);
		json.put("data", jsonObject);
		
		}else {
			json.put("code", 500);
			json.put("Message", "未登录状态");
			
		}
		out.print(json);
		out.flush();
		out.close();
		System.gc();// 执行垃圾回收
	}
	
	
	/**
	 *@author huangqin
	 *  获取用户下的所有设备
	 * 2020年2月19日
	 * @throws IOException 
	 */
	public void getAllEquipmentByUserId() throws IOException {
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out;
		out = ServletActionContext.getResponse().getWriter();

		JsonConfig jsonConfig = new JsonConfig();
		JSONObject json = new JSONObject();
		String userId=ServletActionContext.getRequest().getParameter(
				"userId");
		if(userId==null||userId=="") {
			code = 500;
			MessageStr = "用户id不存在或为空！";
			json.put("code", code);
			json.put("Message", MessageStr);
			out.print(json);
			out.flush();
			out.close();
		}else {
			List<Object> Equipments=equipmentService.getAllEquipmentByUserId(userId);
			
			EquipmentInfo equipmentInfo = null;// 创建实体类
			List<EquipmentInfo> mylist = new LinkedList<EquipmentInfo>();

			Iterator<Object> it = Equipments.iterator();
			while (it.hasNext()) {
				equipmentInfo = new EquipmentInfo();
				Object[] objs = (Object[]) it.next();
				equipmentInfo.setEquipmentId(String.valueOf(objs[0]));
				equipmentInfo.setEquipmentName(String.valueOf(objs[1]));
				equipmentInfo.setEquipmentNote(String.valueOf(objs[2]));
				equipmentInfo.setEquipmentLabel(String.valueOf(objs[3]));
				equipmentInfo.setSpecificEquipmentLabel(String
						.valueOf(objs[4]));
				equipmentInfo.setProductId(String.valueOf(objs[5]));
				equipmentInfo.setProductIcon(String.valueOf(objs[6]));
				equipmentInfo.setProductImage(String.valueOf(objs[7]));
				equipmentInfo.setEquipmentState(String.valueOf(objs[8]));
				equipmentInfo.setRole(String.valueOf(objs[9]));
				if (objs[10] == "" || objs[10] == null) {
					equipmentInfo.setWarnValue("30");
				} else {
					equipmentInfo.setWarnValue(String.valueOf(objs[10]));
				}

				if (objs[11] == "" || objs[11] == null) {
					equipmentInfo.setBlackout("15");
				} else {
					equipmentInfo.setBlackout(String.valueOf(objs[11]));
				}

				if (objs[12] == "" || objs[12] == null) {
					equipmentInfo.setWarnOperate("0");
				} else {
					equipmentInfo.setWarnOperate(String.valueOf(objs[12]));
				}
				mylist.add(equipmentInfo);
			}

			
			JSONArray jsonObject = null;
			jsonObject = JSONArray.fromObject(mylist, jsonConfig);// 转json
			code = 200;
			MessageStr = "success";
			
			json.put("code", code);
			json.put("Message", MessageStr);
			json.put("data", jsonObject);
			out.print(json);
			out.flush();
			out.close();
		}
		
	}
	
	
	public void tuisong() {
		Map<String, String> parm =new HashMap<String, String>();
		Map<String, String> jsonStr =new HashMap<String, String>();
		
		jsonStr.put("name", "smoke_Data");
		jsonStr.put("equipmentId", "zcz002100015");
		jsonStr.put("productId", "zcz002");
		jsonStr.put("model", "warning");
		jsonStr.put("msg", "当前易燃气体浓度为 50%,已超标!");
		
		String msg="当前易燃气体浓度为 "+50+"%,已超标!";
		parm.put("msg",msg);
		  
		  String appKey = "14f406a422b73b9c6cc3c435";
	       String masterSecret = "708ab1213b1160036d9d29c9";
//			System.out.println("推送的用户有："+userList);
//			String productId=json.get("productId");
//			String messageId=json.get("messageId");
	       //创建JPushClient
	       JPushClient jpushClient = new JPushClient(masterSecret, appKey);
	       PushPayload payload = PushPayload.newBuilder()
	            .setPlatform(Platform.all())//ios.android平台的用户
	            .setAudience(Audience.tag("minApp100042"))//通过标签推送用户
	            .setNotification(Notification.newBuilder()
	             //指定当前推送的android通知 
	            .addPlatformNotification(AndroidNotification.newBuilder()
	                            .setAlert(parm.get("msg"))
	                            .build())
	             //指定当前推送的ios通知 
	            .addPlatformNotification(IosNotification.newBuilder()
	                            .setAlert(parm.get("msg"))
	                            .setBadge(1)
	                            .setSound("happy")//这里是设置提示音(更多可以去官网看看)
	                            .addExtras(jsonStr)
	                            .build())
	                    .build())
	            .setOptions(Options.newBuilder().setApnsProduction(false).build())
	            .setMessage(Message.newBuilder().setMsgContent(parm.get("msg"))
	            	 .addExtra("name", jsonStr.get("name"))
                    .addExtra("equipmentId", jsonStr.get("equipmentId"))
                    .addExtra("msg", jsonStr.get("msg"))
                    .addExtra("productId", "zcz002")
                    .addExtra("messageId", "11")
                    .addExtra("model", "warning")
                    .addExtra("msg", jsonStr.get("msg"))
	            	 .addExtras(parm).build()
	            		
	            )//自定义信息
	            .build();

	       try {
				PushResult pu = jpushClient.sendPush(payload);
            System.out.println("推送结果是："+pu);
	        } catch (APIConnectionException e) {
	            e.printStackTrace();
	        } catch (APIRequestException e) {
	            e.printStackTrace();
	        }    
	   }

	/**
	 *@author huangqin 申请授权 2019年9月27日
	 * @throws IOException
	 */
	public void grantApplication() throws IOException {

		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setExcludes(new String[] { "equipment", "openUser" });// 在这里添加要过滤的属性名
		jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);

		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out;
		out = ServletActionContext.getResponse().getWriter();

		JSONObject json = new JSONObject();

		String productId = ServletActionContext.getRequest().getParameter(
				"productId");
		String userId = ServletActionContext.getRequest()
				.getParameter("userId");
		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");

		String sql = "SELECT a from Intermediate a where a.equipmentId='"
				+ equipmentId + "' and a.role='1'";
		List<Object> list04 = equipmentService.selectEquipmentByHql(sql);

		Intermediate intermediate = new Intermediate();
		OpenUser openUser = new OpenUser();
		OpenUser openUser02 = new OpenUser();
		intermediate = (Intermediate) list04.get(0);

		openUser = userService.selectNamebyId(userId);
		openUser02 = userService.selectNamebyId(intermediate.getUserId());
		String msg;
		Map<String, String> parm = new HashMap<String, String>();

		if (openUser.getNickName() != null
				&& openUser.getNickName().toString() != "") {
			msg = "用户 " + openUser.getNickName() + " 向您请求 "
					+ intermediate.getEquipmentNote() + " 授权!";
		} else if (openUser.getPhone() != null && openUser.getPhone() != "") {
			msg = "用户 " + openUser.getPhone() + " 向您请求 "
					+ intermediate.getEquipmentNote() + " 授权!";
		} else {
			msg = "用户 " + openUser.getUserId() + " 向您请求 "
					+ intermediate.getEquipmentNote() + " 授权!";
		}
		parm.put("msg", msg);

		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");// 设置日期格式
		Date now = new Date();
		MessageManage messageManage = new MessageManage();
		messageManage.setEquipmentId(equipmentId);
		messageManage.setProductId(productId);
		messageManage.setMessageContent(msg);
		messageManage.setUserId(openUser02.getUserId());
		messageManage.setMessageTime(sdf.format(now.getTime()));
		messageManage.setMessageType("5");
		messageManage.setReadIf("2");
		messageManage.setOperationIf("2");
		// 添加分享信息
		int messageId = messageManageService.addMessage(messageManage);

		Map<String, String> jsonStr = new HashMap<String, String>();
		jsonStr.put("equipmentId", equipmentId);
		jsonStr.put("productId", productId);
		jsonStr.put("userId", userId);
		jsonStr.put("msg", msg);
		// jsonStr.put("way", "push");
		jsonStr.put("model", "Grant_application");
		jsonStr.put("messageId", String.valueOf(messageId));

		System.out
				.println("openUser02.getUserId()===" + openUser02.getUserId());

		// 设置好账号的app_key和masterSecret是必须的
		String appKey = "14f406a422b73b9c6cc3c435";
		String masterSecret = "708ab1213b1160036d9d29c9";
		// System.out.println("推送的用户有："+userList);
		// 创建JPushClient
		JPushClient jpushClient = new JPushClient(masterSecret, appKey);
		PushPayload payload = PushPayload.newBuilder().setPlatform(
				Platform.all())
				// ios.android平台的用户
				.setAudience(Audience.tag(openUser02.getUserId()))
				// 通过标签推送用户
				.setNotification(
						Notification.newBuilder()
								// 指定当前推送的android通知
								.addPlatformNotification(
										AndroidNotification.newBuilder()
												.setAlert(msg).build())
								// 指定当前推送的ios通知
								.addPlatformNotification(
										IosNotification.newBuilder().setAlert(
												msg).setBadge(1).setSound(
												"happy")// 这里是设置提示音(更多可以去官网看看)
												.addExtras(jsonStr).build())
								.build()).setOptions(
						Options.newBuilder().setApnsProduction(true).build())
				.setMessage(
						Message.newBuilder().setMsgContent(msg).addExtra(
								"equipmentId", equipmentId).addExtra(
								"productId", productId).addExtra("userId",
								userId)
						// .addExtra("way", "push")
								.addExtra("model", "Grant_application")
								// .addExtra("messageId", messageId)
								.addExtra("msg", msg).addExtras(parm).build())// 自定义信息
				.build();

		try {
			// 添加分享记录
			// equipmentService.ShareRecord(userId2,userId,equipmentId);
			PushResult pu = jpushClient.sendPush(payload);
			System.out.println("推送结果是：" + pu);
			code = 200;
			MessageStr = "请求授权成功！";
			json.put("code", code);
			json.put("Message", MessageStr);
			out.print(json);
			out.flush();
			out.close();
		} catch (APIConnectionException e) {
			e.printStackTrace();
		} catch (APIRequestException e) {
			e.printStackTrace();
		}

	}

	/**
	 *@author huangqin 修改设备定位 2019年9月28日
	 * @throws IOException
	 */
	public void updLocation() throws IOException {
		JSONObject json = new JSONObject();
		
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();
		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		String locationStr = ServletActionContext.getRequest().getParameter(
				"locationStr");
		String longitude = ServletActionContext.getRequest().getParameter(
				"longitude");
		String latitude = ServletActionContext.getRequest().getParameter(
				"latitude");
		
		
//		DecimalFormat df = new DecimalFormat("#.00000000000000");
//		double latitudeInt = Double.valueOf(latitude);
//    	
//    	String temp = df.format(latitudeInt);
//    	latitudeInt = Double.valueOf(temp)+0.0036;
//    	System.out.println("latitudeInt==="+latitudeInt);
//    	
//    	double longitudeInt = Double.valueOf(longitude);
//    	String temp02 = df.format(longitudeInt);
//    	longitudeInt = Double.valueOf(temp02)+0.011999;
//    	System.out.println("longitudeInt==="+longitudeInt);
		
//		String formatted_address=getposition(latitudeInt,longitudeInt);
		equipmentService.updLocation(equipmentId, locationStr, longitude,
				latitude);
		code = 200;
		MessageStr = "修改成功！";
		json.put("code", code);
		json.put("Message", MessageStr);
		out.print(json);
		out.flush();
		out.close();
	}
	
	public  String getposition( double lat,double lng) {
		String location=lat+","+lng;
		BufferedReader in = null;
		URL tirc = null;
		String formatted_address=null;
		try {
			tirc = new URL("http://api.map.baidu.com/geocoder/v2/?ak=02jjq8QaGozO8u2cO41Fw4Ku9GUn5iqG&location="+location+"&output=json&pois=0");
			System.out.println(tirc);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			URLConnection connection = tirc.openConnection();
			connection.setDoOutput(true);
			in = new BufferedReader(new InputStreamReader(tirc.openStream(), "UTF-8"));
			String res;
			StringBuilder sb = new StringBuilder("");
			while ((res = in.readLine()) != null) {
				sb.append(res.trim());
			}
			String str = sb.toString();
			 System.out.println(str);
			 JSONObject jo =JSONObject.fromObject(str);
			 jo=(JSONObject) jo.get("result");
			 System.out.println("jsonObject=="+jo.get("formatted_address"));
			 formatted_address=jo.get("formatted_address").toString();
             
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return formatted_address;
	}

	/**
	 *@author huangqin 修改设备信息,增加设备 Mar 29, 2019
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void updateOrAddEquipment() throws IOException {
		JSONObject json = new JSONObject();
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();

		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		String productId = ServletActionContext.getRequest().getParameter(
				"productId");
		String userId = ServletActionContext.getRequest()
				.getParameter("userId");
		String equipmentLabel = ServletActionContext.getRequest().getParameter(
				"equipmentLabel");
		String specificEquipmentLabel = ServletActionContext.getRequest()
				.getParameter("specificEquipmentLabel");
		String equipmentNote = ServletActionContext.getRequest().getParameter(
				"equipmentNote");
		String warnValue = ServletActionContext.getRequest().getParameter(
				"warnValue");
		String blackout = ServletActionContext.getRequest().getParameter(
				"blackout");
		String warnOperate = ServletActionContext.getRequest().getParameter(
				"warnOperate");// 警告操作
//		String equipmentMac= ServletActionContext.getRequest().getParameter(
//				"equipmentMac");
		
		
		
		//设备标签写错,将第6位0改成1  -----开始-------
		String equipmentIdStr = equipmentId.substring(6, 7);
		StringBuffer sb = new StringBuffer(equipmentId);
		if (equipmentIdStr.equals("0")) {
			sb.replace(6, 7, "1");
			equipmentId = sb.toString();
		}
		System.out.println("处理后的设备id为：" + equipmentId);
		//设备标签写错,将第6位0改成1  -----结束-------

		
		if (warnValue != null && warnValue != "") {//修改警告值
			ClientMQTT client = new ClientMQTT();
			String Message="WY+EEPROM=CO_DATA:"+warnValue;
			client.publishMessage(productId, equipmentId, Message);// 发送指令
		}
		
		if (warnOperate != null && warnOperate != "") {//修改警告操作
			ClientMQTT client = new ClientMQTT();
			String Message="";
			if(warnOperate.equals("0")) {
				Message="WY+OVER_DO=2";
			}else if(warnOperate.equals("2")){
				Message="WY+OVER_DO=0";
			}else {
				Message="WY+OVER_DO=1";
			}
			client.publishMessage(productId, equipmentId,Message);// 发送指令
		}

		String way = ServletActionContext.getRequest().getParameter("way");
		String hql = "SELECT d FROM Intermediate d WHERE  d.userId='" + userId
				+ "' and d.productId='" + productId + "' and d.equipmentId='"
				+ equipmentId + "'";
		List a = equipmentService.selectEquipmentByHql(hql);

		if (a.size() > 0) {// 已经绑定，更新设备
			Intermediate intermediate = new Intermediate();
			intermediate = (Intermediate) a.get(0);
			int intermediateId = intermediate.getIntermediateId();
			String IntHql = "SELECT d FROM Intermediate d WHERE  d.equipmentNote='"
					+ equipmentNote
					+ "' and d.userId='"
					+ userId
					+ "' and d.IntermediateId!='" + intermediateId + "'";// 判断设备名称是否存在
			List aEquipmentNote = equipmentService.Intermediates(IntHql);

			if (aEquipmentNote.size() > 0) {
				code = 500;
				MessageStr = "该设备名称已存在！";
			} else {
				String queryString = "update Intermediate a set a.equipmentNote='"
						+ equipmentNote
						+ "',a.equipmentLabel='"
						+ equipmentLabel
						+ "',a.specificEquipmentLabel='"
						+ specificEquipmentLabel
						+ "',a.warnValue='"
						+ warnValue
						+ "',a.blackout='"
						+ blackout
						+ "',a.warnOperate='"
						+ warnOperate
						+ "' where a.equipmentId='"
						+ equipmentId
						+ "' and a.userId='"
						+ userId
						+ "' and a.productId='"
						+ productId + "'";
				equipmentService.updateEquipmentByhql(queryString);
				code = 200;
				MessageStr = "更新设备成功！";
			}
		} else {// 未绑定，新增设备
			String qeHql = "SELECT d FROM Intermediate d WHERE d.userId='"
					+ userId + "' and d.equipmentNote='" + equipmentNote + "'";
			List aEquipmentNote = equipmentService.Intermediates(qeHql);
			if (aEquipmentNote.size() > 0) {
				code = 500;
				MessageStr = "该设备名称已存在！";

			} else {
				String productIdStr = productId.substring(0, 3);
				String productIdStr2 = productId.substring(0, 6);
				Intermediate intermediate = new Intermediate();
				
				String creationDate = String.valueOf(new Date().getTime());
				intermediate.setUserId(userId);
				intermediate.setProductId(productId);
				intermediate.setEquipmentId(equipmentId);
				intermediate.setEquipmentLabel(equipmentLabel);
				intermediate.setSpecificEquipmentLabel(specificEquipmentLabel);
				intermediate.setEquipmentNote(equipmentNote);
				intermediate.setCreationDate(creationDate);
				intermediate.setEquipmentMac("df3c66db8eded811");//设备mac 暂时写死
				intermediate.setBlackout("15");
				intermediate.setWarnOperate("0");
				System.out.println("way======" + way);
				if (way == null || way == "") {
					intermediate.setRole("1");
					if (productIdStr.equals("zcz")) { // 智能插座添加充电保护
						ChargeGuard chargeGuard = new ChargeGuard();
						chargeGuard.setEquipmentId(equipmentId);
						chargeGuard.setUserId(userId);
						chargeGuard.setHmstate(2);
						chargeGuard.setPhstate(2);
						chargeGuard.setVoicestate(1);
						chargeGuardService.addChargeGuard(chargeGuard);
					}
					if (productIdStr2.equals("zcz002")) {// co默认值10%
						intermediate.setWarnValue("30");
					} else if (productIdStr2.equals("zcz003")) {// 易燃气体默认值20%
						intermediate.setWarnValue("30");
					}
				} else {
					intermediate.setRole("2");
				}
				equipmentService.saveIntermediate(intermediate);
				code = 200;
				MessageStr = "新增设备成功！";
				AliGenie(userId);
			}
		}
		json.put("code", code);
		json.put("Message", MessageStr);
		out.print(json);
		out.flush();
		out.close();
		System.gc();// 执行垃圾回收
	}

	/**
	 *@author huangqin 分享设备 Jun 11, 2019
	 * @throws IOException
	 */
	public void share() throws IOException {
		JSONObject json = new JSONObject();
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();
		String phone = ServletActionContext.getRequest().getParameter("phone");

		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		String productId = ServletActionContext.getRequest().getParameter(
				"productId");
		String userId = ServletActionContext.getRequest()
				.getParameter("userId");

		String msg;

		String userId2 = getUserId(phone);
		userId2 = userId2.substring(1, userId2.lastIndexOf("]"));

		OpenUser openUser = new OpenUser();
		openUser = userService.selectNamebyId(userId);
		String nameStr;
		if (openUser.getNickName() != null
				&& openUser.getNickName().toString() != "") {
			nameStr = openUser.getNickName().toString();
		} else if (openUser.getPhone() != null && openUser.getPhone() != "") {
			nameStr = openUser.getPhone();
		} else {
			nameStr = openUser.getUserId();
		}

		if (userId2.equals(userId)) {
			code = 500;
			MessageStr = "无法将设备分享给自己！";
			json.put("code", code);
			json.put("Message", MessageStr);
			out.print(json);
			out.flush();
			out.close();
			System.gc();// 执行垃圾回收

		} else if (userId2.length() == 0) {
			if (phone.length() < 6) {
				MessageStr = "该用户不存在！";
			} else {
				String phoneStr = phone.substring(0, 6);
				if (phoneStr.equals("minApp")) {
					MessageStr = "该用户不存在！";
				} else if (phone.length() != 11) {
					MessageStr = "该用户不存在！";
				} else {

					MessageStr = "该用户未注册或未绑定手机号！";
				}
			}
			code = 500;
			json.put("code", code);
			json.put("Message", MessageStr);
			out.print(json);
			out.flush();
			out.close();
			System.gc();// 执行垃圾回收

		} else {

			Map<String, String> parm = new HashMap<String, String>();
			msg = "用户 " + nameStr + " 向您分享了一个设备,点击确定添加！";
			parm.put("msg", msg);

			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");// 设置日期格式
			Date now = new Date();

			MessageManage messageManage = new MessageManage();
			messageManage.setEquipmentId(equipmentId);
			messageManage.setProductId(productId);
			messageManage.setMessageContent(msg);
			messageManage.setUserId(userId2);
			messageManage.setMessageTime(sdf.format(now.getTime()));
			messageManage.setMessageType("2");
			messageManage.setReadIf("2");
			messageManage.setOperationIf("2");
			// 添加分享信息
			int messageId = messageManageService.addMessage(messageManage);

			Map<String, String> jsonStr = new HashMap<String, String>();
			jsonStr.put("equipmentId", equipmentId);
			jsonStr.put("productId", productId);
			jsonStr.put("msg", msg);
			jsonStr.put("way", "push");
			jsonStr.put("model", "save");
			jsonStr.put("messageId", String.valueOf(messageId));

			System.out.println("userId2=========" + userId2);

			// 设置好账号的app_key和masterSecret是必须的
			String appKey = "14f406a422b73b9c6cc3c435";
			String masterSecret = "708ab1213b1160036d9d29c9";
			// System.out.println("推送的用户有："+userList);
			// 创建JPushClient
			JPushClient jpushClient = new JPushClient(masterSecret, appKey);
			PushPayload payload = PushPayload.newBuilder()
					.setPlatform(Platform.all())
					// ios.android平台的用户
					.setAudience(Audience.tag(userId2))
					// 通过标签推送用户
					.setNotification(
							Notification
									.newBuilder()
									// 指定当前推送的android通知
									.addPlatformNotification(
											AndroidNotification
													.newBuilder()
													.setAlert(
															"用户 "
																	+ nameStr
																	+ " 向您分享了一个设备,点击确定添加！")
													.build())
									// 指定当前推送的ios通知
									.addPlatformNotification(
											IosNotification
													.newBuilder()
													.setAlert(
															"用户 "
																	+ nameStr
																	+ " 向您分享了一个设备,点击确定添加！")
													.setBadge(1).setSound(
															"happy")// 这里是设置提示音(更多可以去官网看看)
													.addExtras(jsonStr).build())
									.build()).setOptions(
							Options.newBuilder().setApnsProduction(true)
									.build()).setMessage(
							Message.newBuilder().setMsgContent(
									"用户 " + nameStr + " 向您分享了一个设备,点击确定添加！")
									.addExtra("equipmentId", equipmentId)
									.addExtra("productId", productId).addExtra(
											"way", "push").addExtra("model",
											"save").addExtra("messageId",
											messageId).addExtra(
											"msg",
											"用户 " + nameStr
													+ " 向您分享了一个设备,点击确定添加！")
									.addExtras(parm).build()

					)// 自定义信息
					.build();
			try {
				// 添加分享记录
				equipmentService.ShareRecord(userId2, userId, equipmentId);
				PushResult pu = jpushClient.sendPush(payload);

				code = 200;
				MessageStr = "分享成功！";
				json.put("code", code);
				json.put("Message", MessageStr);
				out.print(json);
				out.flush();
				out.close();
				System.gc();// 执行垃圾回收
				System.out.println("推送结果是：" + pu);
			} catch (APIConnectionException e) {
				code = 500;
				MessageStr = "分享成功！";
				json.put("code", code);
				json.put("Message", MessageStr);
				out.print(json);
				out.flush();
				out.close();
				System.gc();// 执行垃圾回收
				e.printStackTrace();
			} catch (APIRequestException e) {
				code = 500;
				MessageStr = "分享成功！";
				json.put("code", code);
				json.put("Message", MessageStr);
				out.print(json);
				out.flush();
				out.close();
				System.gc();// 执行垃圾回收
				e.printStackTrace();
			}
		}

	}

	/**
	 *@author huangqin 通过手机号获取用户id Jun 11, 2019
	 */
	public String getUserId(String phone) {
		String userId = null;
		String hql = null;
		String userStr = "null";
		if (phone.length() > 6) {
			userStr = phone.substring(0, 6);
		}
		System.out.println("userStr====" + userStr);
		if (userStr.equals("minApp")) {
			hql = "SELECT a.userId FROM OpenUser a  WHERE a.userId='" + phone
					+ "'";
		} else {
			hql = "SELECT a.userId FROM OpenUser a  WHERE a.phone='" + phone
					+ "'";
		}
		userId = equipmentService.selectEquipmentByHql(hql).toString();
		return userId;
	}

	/**
	 *@author huangqin 设备解除绑定 Mar 30, 2019
	 * @throws IOException
	 */
	public void deleteEquipment() throws IOException {
		JSONObject json = new JSONObject();
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();
		// Product Product=new Product();

		// 设置好账号的app_key和masterSecret是必须的
		String appKey = "14f406a422b73b9c6cc3c435";
		String masterSecret = "708ab1213b1160036d9d29c9";

		String queryString = null;
		String queryShare = null;
		String queryShareAll = null;

		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		String productId = ServletActionContext.getRequest().getParameter(
				"productId");
		String userId = ServletActionContext.getRequest()
				.getParameter("userId");

		String secondUserId = ServletActionContext.getRequest().getParameter(
				"secondUserId");// 次权限用户id

		String hql = "SELECT a from Intermediate a where a.equipmentId='"
				+ equipmentId + "' and a.userId='" + userId
				+ "' and a.role='1'";
		List<Object> list = equipmentService.Intermediates(hql);

		if (list.size() == 0) {
			queryString = "DELETE from Intermediate a where a.equipmentId='"
					+ equipmentId + "' and a.userId='" + userId
					+ "' and a.productId='" + productId + "'";
			equipmentService.deleteRecord(queryString);
			timingService.deleteTimings(equipmentId, userId);// 删除定时任务
			timeDelayService.deleteTimeDelays(equipmentId, secondUserId);// 删除延时任务
			equipmentService.deleteInfrared(equipmentId,userId);//次权限删除插座后只删除自己已绑定插座的红外设备
			AliGenie(userId);//上报给天猫精灵
		} else {

			if (secondUserId != null && secondUserId != "") {

				String equipmentName = equipmentService.equipmentName(
						secondUserId, equipmentId);

				if (equipmentName.equals("false")) {// 已分享但是未添加
					queryShare = "DELETE from ShareRecord a where a.equipmentId='"
							+ equipmentId
							+ "' and a.userIdRoot='"
							+ userId
							+ "' and a.userId='" + secondUserId + "'";// 删除分享记录
					equipmentService.deleteRecord(queryShare);

				} else {
					queryString = "DELETE from Intermediate a where a.equipmentId='"
							+ equipmentId
							+ "' and a.userId='"
							+ secondUserId
							+ "' and a.productId='" + productId + "'";// 主权限删除次权限设备
					queryShare = "DELETE from ShareRecord a where a.equipmentId='"
							+ equipmentId
							+ "' and a.userIdRoot='"
							+ userId
							+ "' and a.userId='" + secondUserId + "'";// 删除分享记录
					equipmentService.deleteRecord(queryString);
					equipmentService.deleteRecord(queryShare);
					equipmentService.deleteInfrared(equipmentId,secondUserId);//主权解除此权限设备后   删除其绑定的红外设备
					
					Map<String, String> parm = new HashMap<String, String>();
					Map<String, String> jsonStr = new HashMap<String, String>();

					SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");// 设置日期格式
					String msg = "您的设备 " + equipmentName + " 已被管理员解除授权！";
					Date now = new Date();
					MessageManage messageManage = new MessageManage();
					messageManage.setEquipmentId(equipmentId);
					messageManage.setProductId(productId);
					messageManage.setMessageType("3");
					messageManage.setReadIf("2");
					messageManage.setOperationIf("2");
					messageManage.setMessageContent(msg);
					messageManage.setUserId(secondUserId);
					messageManage.setMessageTime(sdf.format(now.getTime()));
					int messageId = messageManageService
							.addMessage(messageManage);

					parm.put("msg", msg);
					jsonStr.put("msg", msg);
					jsonStr.put("model", "delete");
					jsonStr.put("messageId", String.valueOf(messageId));
					// 推送消息到被删除设备的用户
					// System.out.println("推送的用户有："+userList);
					// 创建JPushClient
					JPushClient jpushClient = new JPushClient(masterSecret,
							appKey);
					PushPayload payload = PushPayload
							.newBuilder()
							.setPlatform(Platform.all())
							// ios.android平台的用户
							.setAudience(Audience.tag(secondUserId))
							// 通过标签推送用户
							.setNotification(
									Notification
											.newBuilder()
											// 指定当前推送的android通知
											.addPlatformNotification(
													AndroidNotification
															.newBuilder()
															.setAlert(
																	"您的设备 "
																			+ equipmentName
																			+ " 已被管理员解除授权！")
															.build())
											// 指定当前推送的ios通知
											.addPlatformNotification(
													IosNotification
															.newBuilder()
															.setAlert(
																	"您的设备 "
																			+ equipmentName
																			+ " 已被管理员解除授权！")
															.setBadge(1)
															.setSound("happy")
															// 这里是设置提示音(更多可以去官网看看)
															.addExtras(jsonStr)
															.build()).build())
							.setOptions(
									Options.newBuilder()
											.setApnsProduction(true).build())
							.setMessage(
									Message.newBuilder().setMsgContent(
											"您的设备 " + equipmentName
													+ " 已被管理员解除授权！").addExtra(
											"equipmentId", equipmentId)
											.addExtra("productId", productId)
											.addExtra("way", "push").addExtra(
													"msg",
													"您的设备 " + equipmentName
															+ " 已被管理员解除授权！")
											.addExtra("model", "delete")
											.addExtras(parm).build()

							)// 自定义信息
							.build();
					try {
						// 添加分享记录
						PushResult pu = jpushClient.sendPush(payload);
						System.out.println("推送结果是：" + pu);
					} catch (APIConnectionException e) {
						e.printStackTrace();
					} catch (APIRequestException e) {
						e.printStackTrace();
					}
				}

			} else {
				
				queryShareAll = "select a from Intermediate a where a.equipmentId='"
						+ equipmentId + "' and a.role='2'";// 查询所有绑定了
				List<Object> userList = equipmentService
						.Intermediates(queryShareAll);

				queryString = "DELETE from Intermediate a where a.equipmentId='"
						+ equipmentId + "'";// 主权限删除设备，连带次权限下的设备一起删除
				queryShare = "DELETE from ShareRecord a where a.equipmentId='"
						+ equipmentId + "' and a.userIdRoot='" + userId + "'";// 删除所有用户下的分享记录
				equipmentService.deleteRecord(queryShare);
				equipmentService.deleteRecord(queryString);
				timingService.deleteTimings(equipmentId);// 删除定时任务
				timeDelayService.deleteTimeDelays(equipmentId);// 删除延时任务
				chargeGuardService.delChargeGuard(equipmentId);// 删除充电保护
				powerCountService.delPower(equipmentId);//删除电量历史数据
				
				equipmentService.deleteInfrared(equipmentId);//主权限删除所有已绑定插座的红外设备
				AliGenie(userId);

				Map<String, String> parm = new HashMap<String, String>();
				Map<String, String> jsonStr = new HashMap<String, String>();

				// 推送消息到被删除设备的用户
				// System.out.println("推送的用户有："+userList);
				// 创建JPushClient
				if (userList.size() > 0) {
					Intermediate intermediate = new Intermediate();
					for (int i = 0; i < userList.size(); i++) { // 循环推送

						intermediate = (Intermediate) userList.get(i);
						AliGenie(intermediate.getUserId());//上报给天猫精灵

						SimpleDateFormat sdf = new SimpleDateFormat(
								"MM-dd HH:mm");// 设置日期格式
						Date now = new Date();

						String msg = "您的设备 " + intermediate.getEquipmentNote()
								+ " 已被管理员解除授权！";

						MessageManage messageManage = new MessageManage();
						messageManage.setEquipmentId(equipmentId);
						messageManage.setProductId(productId);
						messageManage.setMessageType("3");
						messageManage.setReadIf("2");
						messageManage.setOperationIf("2");
						messageManage.setMessageContent(msg);
						messageManage.setUserId(secondUserId);
						messageManage.setMessageTime(sdf.format(now.getTime()));
						int messageId = messageManageService
								.addMessage(messageManage);

						parm.put("msg", msg);
						jsonStr.put("msg", msg);
						jsonStr.put("model", "delete");
						jsonStr.put("messageId", String.valueOf(messageId));

						JPushClient jpushClient = new JPushClient(masterSecret,
								appKey);
						PushPayload payload = PushPayload
								.newBuilder()
								.setPlatform(Platform.all())
								// ios.android平台的用户
								.setAudience(
										Audience.tag(intermediate.getUserId()))
								// 通过标签推送用户
								.setNotification(
										Notification
												.newBuilder()
												// 指定当前推送的android通知
												.addPlatformNotification(
														AndroidNotification
																.newBuilder()
																.setAlert(
																		"您的设备 "
																				+ intermediate
																						.getEquipmentNote()
																				+ " 已被管理员解除授权！")
																.build())
												// 指定当前推送的ios通知
												.addPlatformNotification(
														IosNotification
																.newBuilder()
																.setAlert(
																		"您的设备 "
																				+ intermediate
																						.getEquipmentNote()
																				+ " 已被管理员解除授权！")
																.setBadge(1)
																.setSound(
																		"happy")
																// 这里是设置提示音(更多可以去官网看看)
																.addExtras(
																		jsonStr)
																.build())
												.build())
								.setOptions(
										Options.newBuilder().setApnsProduction(
												true).build())
								.setMessage(
										Message
												.newBuilder()
												.setMsgContent(
														"您的设备 "
																+ intermediate
																		.getEquipmentNote()
																+ " 已被管理员解除授权！")
												.addExtra("equipmentId",
														equipmentId)
												.addExtra("productId",
														productId)
												.addExtra("way", "push")
												.addExtra(
														"msg",
														"您的设备 "
																+ intermediate
																		.getEquipmentNote()
																+ " 已被管理员解除授权！")
												.addExtra("model", "delete")
												.addExtra("messageId",
														messageId).addExtras(
														parm).build()

								)// 自定义信息
								.build();
						try {
							// 添加分享记录
							PushResult pu = jpushClient.sendPush(payload);
							System.out.println("推送结果是：" + pu);
						} catch (APIConnectionException e) {
							e.printStackTrace();
						} catch (APIRequestException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		String hql01;
		if (secondUserId != null && secondUserId != "") {
			hql01 = "SELECT d FROM Intermediate d WHERE  d.userId='"
					+ secondUserId + "' and d.productId='" + productId
					+ "' and d.equipmentId='" + equipmentId + "'";
		} else {
			hql01 = "SELECT d FROM Intermediate d WHERE  d.userId='"
					+ secondUserId + "' and d.productId='" + productId
					+ "' and d.equipmentId='" + equipmentId + "'";
		}
		List<Object> a = equipmentService.Intermediates(hql01);
		if (a.size() > 0) {
			code = 500;
			MessageStr = "删除设备失败！";
		} else {
			code = 200;
			MessageStr = "删除设备成功！";
		}

		json.put("code", code);
		json.put("Message", MessageStr);
		out.print(json);
		out.flush();
		out.close();
		System.gc();// 执行垃圾回收

	}

	/**
	 *@author huangqin 查询次权限设备 Jun 14, 2019
	 * @throws IOException
	 */
	public void subpermission() throws IOException {
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);

		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		String userId = ServletActionContext.getRequest()
				.getParameter("userId");
		List<ShareRecord> subpermissions = equipmentService.subpermission(
				equipmentId, userId);
		List<String> bigSubpermission = equipmentService
				.bigSubpermission(equipmentId);// 主权限账号
		JSONObject json = new JSONObject();
		JSONObject json02 = new JSONObject();
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();

		JSONArray jsonObject = null;
		jsonObject = JSONArray.fromObject(subpermissions, jsonConfig);// 转json

		json02.put("custodian", bigSubpermission.get(0));
		json02.put("custodianHead", bigSubpermission.get(1));
		json02.put("userList", jsonObject);

		code = 200;
		MessageStr = "success";
		json.put("code", code);
		json.put("Message", MessageStr);
		json.put("data", json02);

		out.print(json);
		out.flush();
		out.close();
		System.gc();// 执行垃圾回收

	}

	public void versionData() throws IOException {
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);

		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		PrintWriter out = ServletActionContext.getResponse().getWriter();

		String equipmentId = ServletActionContext.getRequest().getParameter(
				"equipmentId");
		equipmentId = equipmentId.substring(0, 6);
		String version = productService.equVersion(equipmentId);
		JSONObject json = new JSONObject();
		JSONObject json02 = new JSONObject();

		if (equipmentId != null && equipmentId != "") {
			code = 200;
			MessageStr = "success";
			json.put("code", code);
			json.put("Message", MessageStr);
			json02.put("IP", "112.74.48.180");
			json02.put("PORT", 80);
			json02.put("fileBinPath", "/wanYe/file/" + equipmentId + "/");
			json02.put("sermware_version", version);
			json.put("data", json02);
			out.print(json);
			out.flush();
			out.close();
			System.gc();// 执行垃圾回收
		} else {
			code = 500;
			MessageStr = "error";
			json.put("code", code);
			json.put("Message", MessageStr);
			out.print(json);
			out.flush();
			out.close();
		}
	}
	
	
	/**
	 *@author huangqin
	 *  设备删除，添加上报给天猫精灵
	 * 2020年1月20日
	 * @throws ApiException 
	 */
	public void AliGenie(String userId){
		System.out.println("用户id是："+userId);
		String phone=equipmentService.phoneGet(userId);
		String userToken=equipmentService.tokenGet(userId);
		
		System.out.println("根据用户id获取的Token是："+userToken);
		System.out.println("绑定的手机号是："+phone);
		
		if(userToken==null||userToken==""&&phone!=null||phone!="") {
			System.out.println("用户"+userId+"绑定了手机号");
			userToken=equipmentService.tokenGet(phone);
		}
		
		System.out.println("最终Token是："+userToken);
		
		try {
			String appkey="27803406";
			String secret="729ded12a4a9be618be61c1777ae6ec7";
			String url="http://gw.api.taobao.com/router/rest";
			TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
			AlibabaAilabsIotDeviceListUpdateNotifyRequest req = new AlibabaAilabsIotDeviceListUpdateNotifyRequest();
			req.setToken(userToken);
			req.setSkillId("39446");
			AlibabaAilabsIotDeviceListUpdateNotifyResponse rsp;
			rsp = client.execute(req);
			System.out.println(rsp.getBody());
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
}
	

	public EquipmentService getEquipmentService() {
		return equipmentService;
	}

	public void setEquipmentService(EquipmentService equipmentService) {
		this.equipmentService = equipmentService;
	}

	public TimingService getTimingService() {
		return timingService;
	}

	public void setTimingService(TimingService timingService) {
		this.timingService = timingService;
	}

	public TimeDelayService getTimeDelayService() {
		return timeDelayService;
	}

	public void setTimeDelayService(TimeDelayService timeDelayService) {
		this.timeDelayService = timeDelayService;
	}

}
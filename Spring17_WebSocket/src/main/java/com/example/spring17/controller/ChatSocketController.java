package com.example.spring17.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.spring17.anno.SocketController;
import com.example.spring17.anno.SocketMapping;
import com.example.spring17.dto.ChatMessage;
import com.example.spring17.handler.SocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SocketController
public class ChatSocketController {
	@Autowired
	private SocketSessionManager sessionManager;
	// 객체 <=> json 상호 변경할수 있는 객체
	ObjectMapper mapper=new ObjectMapper();
	
	@SocketMapping("/chat/public")
	public void chatPublic(WebSocketSession session, ChatMessage message) {
		String json="""
			{
				"type":"public",
				"payload":{
					"userName":"%s",
					"text":"%s"
				}
			}
		""".formatted(message.getUserName(), message.getText());
		TextMessage msg=new TextMessage(json);
		sessionManager.broadcast(msg);
	}
	
	@SocketMapping("/chat/enter")
	public void chatEnter(WebSocketSession session, ChatMessage message) {
		//대화방에 입장하는 userName 
		String userName=message.getUserName();
		//누가 어떤 session 으로 입장했는지 저장하기 
		sessionManager.enterUser(userName, session);
		//대화방에 입장한 모든 사용자 목록
		List<String> userList=sessionManager.getAllUserNames();
		
		Map<String, Object> map=Map.of(
			"type", "enter",
			"payload", Map.of(
				"userName", userName,
				"userList", userList
			)
		);
		
		//ObjectMapper 객체를 이용해서 Map 에 담긴 내용을 json 문자열로 변환
		String json="{}";
		try {
			json = mapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		//대화방에 입장한 모든 클라이언트에게 전송할 정보 
		TextMessage msg=new TextMessage(json);
		//session manager 객체의 메소드를 이용해서 전송한다.
		sessionManager.broadcast(msg);
	}
	
	
}









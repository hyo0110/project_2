package com.mrs.project.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mrs.project.dto.BoardDTO;
import com.mrs.project.dto.MemberDTO;
import com.mrs.project.service.MemberService;


@Controller
public class MemberController {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired MemberService service;
	@Value("#{config['manager.id']}") String adminId;
	@Value("#{config['manager.pw']}") String adminPw;
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public String index() {
		return "member/index_login";
	}
	
	//로그인-----------------------------------------------------------------------------------------
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ModelAndView login(@RequestParam String id, @RequestParam String pw, HttpSession session, RedirectAttributes rAttr) {
		//logger.info(id+"/"+pw);	
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] byteArrid = digest.digest(id.getBytes(StandardCharsets.UTF_8));
			//logger.info("id : "+byteArrid);
		
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
		}
		ModelAndView mav = new ModelAndView();	
		String page = "member/index_login";	
		String msg = "로그인 실패했습니다.";
		
		if(id.equals(adminId) && pw.equals(adminPw)) {
			session.setAttribute("loginid", adminId);
			page = "redirect:/admin?type=0";
		} else{
			//일반로그인인지
			int cnt = service.login(id,pw);
			if(cnt>0) {
			session.setAttribute("loginid", id);
			session.setAttribute("recent_search", null);
			msg = "로그인 성공했습니다.";
			page = "redirect:/";
			//sns로그인인지(kakaoid, googleid ...)
				/*
				 * int cnt = service.login(id,pw); if(cnt>0) { session.setAttribute("loginid",
				 * id); session.setAttribute("recent_search", null); msg = "로그인 성공했습니다."; page =
				 * "redirect:/";
				 */
		}
			//mav.addObject("msg",msg);
			rAttr.addFlashAttribute("msg", msg);
		}		
		
		mav.setViewName(page);
		
		return mav;
	}
	
	//회원가입폼----------------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/joinForm", method = RequestMethod.GET)
	public String joinForm(Model model) {		
		return "member/index_join";
	}
	
	//회원가입-----------------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/join", method = RequestMethod.POST)
	public ModelAndView join(@RequestParam HashMap<String, Object> params) {
		logger.info("params : {}",params);
		
		return service.join(params);
	}
	
	//아이디 중복확인-----------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/dbchk", method = RequestMethod.GET)
	public @ResponseBody HashMap<String, Object> dbchk(@RequestParam String id) {
		//logger.info("id : {}",id);
		return service.dbchk(id);
	}
	
	//로그아웃------------------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpSession session, Model model) {
		session.removeAttribute("loginid");
		session.removeAttribute("recent_search");
		model.addAttribute("msg","로그아웃 되었습니다.");		
		return "index";
	}
	//카카오 로그아웃 페이지 이동
	@RequestMapping(value = "/kaologout", method = RequestMethod.GET)
	public ModelAndView kaologout(ModelAndView mav,HttpSession session) {
		System.out.println("kao Logout");
		String reqURL ="https://kauth.kakao.com/oauth/logout?client_id=504490bc7bab52d815247c9fa2477533&logout_redirect_uri=http://127.0.0.1:8080/project/logout";
		//service.AcsCode(reqURL);
		mav.addObject("reqURL", reqURL);
		mav.setViewName("member/kaologout");
		return mav;
	}
	
	//마이페이지 재로그인 화면이동-------------------------------------------------------------------------------------------
	@RequestMapping(value = "/mypage_login", method = RequestMethod.GET)
	public String mypage_login(HttpSession session, Model model) {	
		
		return "member/mypage_login";
	}
	
	//마이페이지 재로그인 화면이동-------------------------------------------------------------------------------------------
	@RequestMapping(value = "/mypage_login", method = RequestMethod.POST)
	public String mypage_relogin(HttpSession session,@RequestParam String pw) {	
		String id =(String) session.getAttribute("loginid");
		int same = service.mypage_loginpw(id,pw);
		if(same == 1) {
			return  "redirect:/mypage_detail";
		}else {
			return "redirect:/mypage_login";
		}
	}
	
	//마이페이지 내 정보 상세보기-------------------------------------------------------------------------------------------
	@RequestMapping(value = "/mypage_detail", method = RequestMethod.GET)
	public String mypage_detail(HttpSession session, Model model) { 
		 String id =(String) session.getAttribute("loginid");		 
		MemberDTO dto = service.mypage_detail(id);
		model.addAttribute("member",dto);		
		return "member/mypage_detail";
	}
	//마이페이지 탈퇴하기---------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/deleteMember", method = RequestMethod.GET)
	public String deleteMember(HttpSession session, Model model) {
		String id = (String) session.getAttribute("loginid");
		service.deleteMember(id);
		session.removeAttribute("loginid");
		return "redirect:/logout";
	}
	
	// 내 정보 수정 페이지 들어갈때-------------------------------------------------------------------------------------------
	@RequestMapping(value = "/mypage_update", method = RequestMethod.GET)
	public String mypage_update(HttpSession session, Model model) {
		String id = (String) session.getAttribute("loginid");
		MemberDTO dto = service.mypage_updatepage(id);
		model.addAttribute("member", dto);
		return "member/mypage_update";
	}
	
	// 내 정보 수정하기-----------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/updateMember", method = RequestMethod.GET)
	public String updateMember(Model model, @RequestParam String user_id,
			@RequestParam String user_pw, @RequestParam String user_name, @RequestParam String user_email) {		
		int count = service.updateMember(user_id, user_pw, user_name, user_email);
		//ogger.info("~~~~~");
		String page ="redirect:/mypage_update";
		if(count>0) {
			page="redirect:/mypage_detail";
		}
		return page;
	}
	
	// 스크랩 페이지 들어갈때--------------------------------------------------------------------------------------------------
		@RequestMapping(value = "/mypage_scrap", method = RequestMethod.GET)
		public String mypage_scrap(HttpSession session, Model model) {
			String id = (String) session.getAttribute("loginid");
			service.mypage_scrap(id,model);
			return "member/mypage_scrap";
		}
		
		// 스크랩 페이지 삭제할때-------------------------------------------------------------------------------------------------
		@RequestMapping(value = "/scrap_delete", method = RequestMethod.GET)
		public String scrap_delete(@RequestParam String idx) {
			service.scrap_delete(idx);
			return "redirect:/mypage_scrap";
					
		}
		
		// 내가 쓴 글 들어갈때------------------------------------------------------------------------------------------------------
		@RequestMapping(value = "/mypage_written", method = RequestMethod.GET)
		public String mypage_written(HttpSession session, Model model, @RequestParam int page) {
			String id = (String) session.getAttribute("loginid");

			return service.mypage_written(id, page, model);
		}
		
		//-----------------------------------------------------------------------------------------------------------------------------
		//SNS Social Controll
		
		//인가 코드 받기 ----------------------------------------------------------------------------------------------------------------
		@RequestMapping(value = "/AcsCode", method = RequestMethod.GET)
		public ModelAndView AcsCode(RedirectAttributes rAttr ,ModelAndView mav, HttpSession session, @RequestParam Map<String, Object> params)  {
			//인가코드 URL
			String reqURL ="https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=504490bc7bab52d815247c9fa2477533&redirect_uri=http://127.0.0.1:8080/project/Kakaologin";
			//service.AcsCode(reqURL);
			mav.addObject("reqURL", reqURL);
			mav.setViewName("member/AcsCode");
			return mav;
		}
		
		
		//카카오 로그인-----------------------------------------------------------------------------------------------------------------		
		@RequestMapping(value = "/Kakaologin", method = RequestMethod.GET)
		public ModelAndView Kakaologin(RedirectAttributes rAttr ,ModelAndView mav, HttpSession session, @RequestParam Map<String, Object> params)  {
			
			logger.info("params : " + params);
			String token = service.getAccessToken(params);
			session.setAttribute("AccessToken", token);
			
			System.out.println("controller access_token : " + token);
			String id =service.getUserInfo_kakao(token);
			
			mav.addObject("result", token);
			mav = service.kaoIdChk(id,session ,rAttr);
			return mav;
		}
		
		//SNS와 회원 열결------------------------ ------------------------------------------------------------------------
		@RequestMapping(value = "/MemberConnect", method = RequestMethod.POST)
		public ModelAndView MemberConnect(@RequestParam String id, @RequestParam String pw, HttpSession session, RedirectAttributes rAttr) {
			//logger.info(id+"/"+pw);	
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] byteArrid = digest.digest(id.getBytes(StandardCharsets.UTF_8));
			
			} catch (NoSuchAlgorithmException e) {
				//e.printStackTrace();
			}
			
			ModelAndView mav = new ModelAndView();	
			String page = "member/index_login";	
			String msg = "로그인 실패했습니다.";
			
			if(id.equals(adminId) && pw.equals(adminPw)) {
				session.setAttribute("loginid", adminId);
				page = "redirect:/admin?type=0";
			} else{
				//일반로그인인지
				int cnt = service.login(id,pw);
				if(cnt>0) {
					
				//세션의 kakaoid를 해당 id에 추가
				String kakaoId = (String) session.getAttribute("kakaoId");
				System.out.println(kakaoId);
				int success = service.memberConnect(id, pw, kakaoId);
				if (success>0) {
					
					session.setAttribute("loginid", id);
					session.setAttribute("recent_search", null);
					msg = "로그인 성공했습니다.";
					page = "redirect:/";
				}
				//sns로그인인지(kakaoid, googleid ...)
					/*
					 * int cnt = service.login(id,pw); if(cnt>0) { session.setAttribute("loginid",
					 * id); session.setAttribute("recent_search", null); msg = "로그인 성공했습니다."; page =
					 * "redirect:/";
					 */
			}
				//mav.addObject("msg",msg);
				rAttr.addFlashAttribute("msg", msg);
			}		
			
			mav.setViewName(page);
			
			return mav;
		}
		//네이버 로그인-----------------------------------------------------------------------------------------------------------------
		@RequestMapping(value = "/naverLogin", method = RequestMethod.GET)
		public ModelAndView naverLogin(ModelAndView mav, HttpSession session, @RequestParam Map<String, Object> params)  {
			
			logger.info("params : " + params);
			String result = service.getAccessToken_naver(params);
			System.out.println("controller access_token : " + result);
			
			mav.addObject("result", result);
			mav.setViewName("index");
		
			
			return mav;
		}
		
		//카카오 계정 연결 끊기(사용자 원할 때(선택), 혹은 탈퇴시(필수) 
		@RequestMapping(value = "/disconnect", method = RequestMethod.POST)
		public ModelAndView disconnect(ModelAndView mav, HttpSession session, @RequestParam Map<String, Object> params)  {

			System.out.println("disconnect 컨트롤러-----------------------");
			//System.out.println("access token : "+ session.getAttribute("AccessToken"));
			service.disconnect(session);
			session.removeAttribute("loginid");
			session.removeAttribute("recent_search");
			mav.setViewName("index");
			return mav;
		}
		

}

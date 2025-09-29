package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.validation.Valid;
import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LoginUserDto loginUserDto;
// 河島麻登花 - Task.26 追加分
	@Autowired
	private AttendanceUtil attendanceUtil;
	 /**
     * 勤怠入力画面（update.html）の表示
     */
    @RequestMapping(value = "/update", method = RequestMethod.GET)
    public String showUpdateForm(Model model) {
    	// 勤怠管理リストの取得
        List<AttendanceManagementDto> attendanceManagementDtoList =
            studentAttendanceService.getAttendanceManagement(
                loginUserDto.getCourseId(),
                loginUserDto.getLmsUserId()
            );
        
     // 勤怠フォームの生成（ここで attendanceList に詰める）
        AttendanceForm attendanceForm =
            studentAttendanceService.setAttendanceForm(attendanceManagementDtoList);


        // プルダウン用のデータをセット
     // プルダウン用のデータをセット
        attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
        attendanceForm.setHourMap(attendanceUtil.getHourMap());
        attendanceForm.setMinuteMap(attendanceUtil.getMinuteMap(1));


        // 画面に渡す
        model.addAttribute("attendanceForm", attendanceForm);

        // templates/attendance/update.html を表示
        return "attendance/update";
    }

    /**
     * 勤怠データの更新処理
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public String updateAttendance(
            @ModelAttribute("attendanceForm") @Validated AttendanceForm form,
            BindingResult result,
            Model model)throws ParseException { 

        // サービスで入力チェック
        studentAttendanceService.validateAttendance(form, result);

        // エラーがある場合
        if (result.hasErrors()) {
            // もう一度プルダウンをセットし直す（エラー画面でも表示するため）
            form.setBlankTimes(attendanceUtil.setBlankTime());
            form.setHourMap(attendanceUtil.getHourMap());
            form.setMinuteMap(attendanceUtil.getMinuteMap(1));


            model.addAttribute("attendanceForm", form);
            return "attendance/update";
        }
        // 入力を保存してから遷移
        studentAttendanceService.update(form);
        
        return "redirect:/attendance/detail";
    }
	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) {
		// 勤怠一覧の取得
		List<AttendanceManagementDto> attendanceManagementDtoList = 
				studentAttendanceService.getAttendanceManagement(
						loginUserDto.getCourseId(), 
						loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

	/**
     * 勤怠管理画面（detail）の表示
     * 
     * 過去日の勤怠に未入力がある場合はメッセージを画面に表示する
     *
	 * @author 河島麻登花 – Task.25
	 * @param model
	 * @return 勤怠管理画面
	 */
		// 今日（00:00:00）の Date を Util から取得してる
		Date today = attendanceUtil.getTrainingDate();
		
		boolean notEnterFlg = false;
	    for (AttendanceManagementDto dto : attendanceManagementDtoList) {
	        // “過去日”か？
	        if (dto.getTrainingDate() != null && dto.getTrainingDate().before(today)) {
	            // 出勤 or 退勤のどちらかが空なら未入力扱い
	            boolean startEmpty = (dto.getTrainingStartTime() == null || dto.getTrainingStartTime().isEmpty());
	            boolean endEmpty   = (dto.getTrainingEndTime()   == null || dto.getTrainingEndTime().isEmpty());
	            if (startEmpty || endEmpty) {
	                notEnterFlg = true;
	                break;  //1県見つけた時点で処理を抜ける
	            }
	        }
	    }
	    model.addAttribute("notEnterFlg", notEnterFlg);
        return "attendance/detail";
}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		
		/**
		 * 勤怠管理画面（直接変更画面）の初期表示
		 * 
		 * @author 河島麻登花 - Task.26
		 * @param model
		 * @return 勤怠情報直接変更画面
		 */
		attendanceForm.setHourMap(attendanceUtil.getHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.getMinuteMap(1));
		
		model.addAttribute("attendanceForm", attendanceForm);
		
		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	// 河島麻登花 - Task.27(@Validを付けました)
	public String complete(@Valid AttendanceForm attendanceForm, Model model, BindingResult result)
			throws ParseException {
		
		 // 河島麻登花 - Task.27 入力チェッックを呼び出す
	    studentAttendanceService.validateAttendance(attendanceForm, result);

	    // 河島麻登花 - Task.27 エラーがある場合は「update.html」に戻す
	    if (result.hasErrors()) {
	        model.addAttribute("attendanceForm", attendanceForm);
	        return "attendance/update";
	    }

	    // 河島麻登花 - Task.27 エラーがなければ更新処理
		// 更新
		String message = studentAttendanceService.update(attendanceForm);
		model.addAttribute("message", message);
		
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
}
	
	}
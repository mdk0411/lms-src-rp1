package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;
	
	/**
	 * 過去日未入力の有無をチェック
	 * 
	 * @author 河島麻登花 – Task.25
	 * @return 未入力あればtrue、なければfalse
	 */
public boolean checkNotEnterCount() {
	Date trainingDate = attendanceUtil.getTrainingDate();
	
	Integer count = tStudentAttendanceMapper.notEnterCount(
			loginUserDto.getLmsUserId(),
			Constants.DB_FLG_FALSE,
			trainingDate
	);
	return count != null && 0 < count;
	}

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			
			//Task.26（DBから取った "HH:mm" を Hour と Minute に分解）
		    if (attendanceManagementDto.getTrainingStartTime() != null && attendanceManagementDto.getTrainingStartTime().contains(":")) {
		        String[] p = attendanceManagementDto.getTrainingStartTime().split(":");
		        if (p.length == 2) {
		            dailyAttendanceForm.setTrainingStartTimeHour(p[0]);   // "09"
		            dailyAttendanceForm.setTrainingStartTimeMinute(p[1]); // "00"
		        }
		    }
		    if (attendanceManagementDto.getTrainingEndTime() != null && attendanceManagementDto.getTrainingEndTime().contains(":")) {
		        String[] p = attendanceManagementDto.getTrainingEndTime().split(":");
		        if (p.length == 2) {
		            dailyAttendanceForm.setTrainingEndTimeHour(p[0]);
		            dailyAttendanceForm.setTrainingEndTimeMinute(p[1]);
		        }
		    }
			
			
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			
			
			//  Task.26 時・分を結合して trainingStartTime / trainingEndTime にセット
			if (dailyAttendanceForm.getTrainingStartTimeHour() != null 
			        && !dailyAttendanceForm.getTrainingStartTimeHour().isEmpty()
			        && dailyAttendanceForm.getTrainingStartTimeMinute() != null
			        && !dailyAttendanceForm.getTrainingStartTimeMinute().isEmpty()) {

			    dailyAttendanceForm.setTrainingStartTime(
			        dailyAttendanceForm.getTrainingStartTimeHour() + ":" +
			        dailyAttendanceForm.getTrainingStartTimeMinute()
			    );
			} else {
			    dailyAttendanceForm.setTrainingStartTime(null); // 空なら null
			}

			if (dailyAttendanceForm.getTrainingEndTimeHour() != null 
			        && !dailyAttendanceForm.getTrainingEndTimeHour().isEmpty()
			        && dailyAttendanceForm.getTrainingEndTimeMinute() != null
			        && !dailyAttendanceForm.getTrainingEndTimeMinute().isEmpty()) {

			    dailyAttendanceForm.setTrainingEndTime(
			        dailyAttendanceForm.getTrainingEndTimeHour() + ":" +
			        dailyAttendanceForm.getTrainingEndTimeMinute()
			    );
			} else {
			    dailyAttendanceForm.setTrainingEndTime(null); // 空なら null
			}
			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
	        tStudentAttendance.setStudentAttendanceId(dailyAttendanceForm.getStudentAttendanceId());
	        tStudentAttendance.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
	        tStudentAttendance.setTrainingStartTime(dailyAttendanceForm.getTrainingStartTime());
	        tStudentAttendance.setTrainingEndTime(dailyAttendanceForm.getTrainingEndTime());
	        tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
	        tStudentAttendance.setNote(dailyAttendanceForm.getNote());
	        if (dailyAttendanceForm.getStatus() != null && !dailyAttendanceForm.getStatus().isEmpty()) {
	            tStudentAttendance.setStatus(Short.parseShort(dailyAttendanceForm.getStatus()));
	        }
	        
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			
			// Task.26出勤時刻整形
	        TrainingTime trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
	        tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());

	        // Task.26退勤時刻整形
	        TrainingTime trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
	        tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());


			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
		}
	
	/**
	 * 河島麻登花 - Task.27
	 * 
	 * 入力チェック
	 * 
	 * @param form
	 * @param result
	 */
	public void validateAttendance(AttendanceForm form, BindingResult result) {

	    // 勤怠リストを取得して1件ずつチェックする
	    List<DailyAttendanceForm> list = form.getAttendanceList();

	    for (int i = 0; i < list.size(); i++) {
	        DailyAttendanceForm daily = list.get(i);
	        
	        // a. 備考の文字数チェック → @Size(max=100) により自動バリデーションされるため、ここでは明示しない

	        // b. 出勤時間（時・分）のどちらかだけ入力されている場合
	        if ((daily.getTrainingStartTimeHour() != null && !daily.getTrainingStartTimeHour().isEmpty()
	          && (daily.getTrainingStartTimeMinute() == null || daily.getTrainingStartTimeMinute().isEmpty()))
	         || ((daily.getTrainingStartTimeHour() == null || daily.getTrainingStartTimeHour().isEmpty())
	          && daily.getTrainingStartTimeMinute() != null && !daily.getTrainingStartTimeMinute().isEmpty())) {

	            result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour",
	                    "input.invalid", new Object[]{"出勤時間"}, null);
	        }

	        // c. 退勤時間（時・分）のどちらかだけ入力されている場合
	        if ((daily.getTrainingEndTimeHour() != null && !daily.getTrainingEndTimeHour().isEmpty()
	          && (daily.getTrainingEndTimeMinute() == null || daily.getTrainingEndTimeMinute().isEmpty()))
	         || ((daily.getTrainingEndTimeHour() == null || daily.getTrainingEndTimeHour().isEmpty())
	          && daily.getTrainingEndTimeMinute() != null && !daily.getTrainingEndTimeMinute().isEmpty())) {

	            result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour",
	                    "input.invalid", new Object[]{"退勤時間"}, null);
	        }

	        // d. 出勤が未入力なのに退勤だけ入力されている場合
	        if ((daily.getTrainingStartTimeHour() == null || daily.getTrainingStartTimeHour().isEmpty())
	         && (daily.getTrainingStartTimeMinute() == null || daily.getTrainingStartTimeMinute().isEmpty())
	         && (daily.getTrainingEndTimeHour() != null && !daily.getTrainingEndTimeHour().isEmpty()
	          || daily.getTrainingEndTimeMinute() != null && !daily.getTrainingEndTimeMinute().isEmpty())) {

	            result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour",
	                    "attendance.punchInEmpty", null, null);
	        }

	     // 出勤と退勤の両方が入力されているときだけ
	     // 退勤が出勤より早くないか（e） or 休憩が長すぎないか（f）をチェックする
	        if (daily.getTrainingStartTimeHour() != null && !daily.getTrainingStartTimeHour().isEmpty()
	         && daily.getTrainingStartTimeMinute() != null && !daily.getTrainingStartTimeMinute().isEmpty()
	         && daily.getTrainingEndTimeHour() != null && !daily.getTrainingEndTimeHour().isEmpty()
	         && daily.getTrainingEndTimeMinute() != null && !daily.getTrainingEndTimeMinute().isEmpty()) {

	            // 出勤時間と退勤時間を「分」に直して比較しやすくする
	        	// 例：9時30分 → 570分、17時15分 → 1035分 みたいな感じ
	            int startHour = Integer.parseInt(daily.getTrainingStartTimeHour());
	            int startMinute = Integer.parseInt(daily.getTrainingStartTimeMinute());
	            int endHour = Integer.parseInt(daily.getTrainingEndTimeHour());
	            int endMinute = Integer.parseInt(daily.getTrainingEndTimeMinute());
	            
	            //「時 × 60 + 分」で、分単位に変換する
	            int startTotal = startHour * 60 + startMinute;
	            int endTotal = endHour * 60 + endMinute;

	            // e. 退勤時間 < 出勤時間 の場合エラーを出す
	            if (endTotal < startTotal) {
	                result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour",
	                        "attendance.trainingTimeRange", new Object[]{i}, null);
	            }

	            // f. 中抜け時間が勤務時間を超える場合、エラーを出す
	            if (daily.getBlankTime() != null && daily.getBlankTime() > (endTotal - startTotal)) {
	                result.rejectValue("attendanceList[" + i + "].blankTime",
	                        "attendance.blankTimeError", null, null);
	            }
	        }
	    }

	    // Task.26対応：エラーがあった場合は、プルダウンの選択肢を再セットする
	    if (result.hasErrors()) {
	        form.setBlankTimes(attendanceUtil.setBlankTime());
	        form.setHourMap(attendanceUtil.getHourMap());
	        form.setMinuteMap(attendanceUtil.getMinuteMap(1));
	    }
	}
}
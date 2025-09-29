package jp.co.sss.lms.form;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 日次の勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class DailyAttendanceForm {

	/** 受講生勤怠ID */
	private Integer studentAttendanceId;
	/** 途中退校日 */
	private String leaveDate;
	/** 日付 */
	private String trainingDate;
	
	
	/** 出勤時間（hh:mm 形式で保持） */
	private String trainingStartTime;
	/**
	 * 出勤時間(時)と(分)
	 * 
	 * @author 河島麻登花 - Task.26
	 */
	/** 出勤時間（時） */
		private String trainingStartTimeHour;
	/** 出勤時間（分） */
		private String trainingStartTimeMinute;
	
	/** 退勤時間（hh:mm 形式で保持） */
	private String trainingEndTime;
	/**
	 * 退勤時間(時)と(分)
	 * 
	 * @author 河島麻登花 - Task.26
	 */
	/** 退勤時間（時） */
		private String trainingEndTimeHour;
	/** 退勤時間（分） */
		private String trainingEndTimeMinute;

	/** 中抜け時間 */
	private Integer blankTime;
	/** 中抜け時間（画面表示用） */
	private String blankTimeValue;
	/** ステータス */
	private String status;

	/**
	 * 備考に@Sizeを付与したる
	 * 
	 * @author 河島麻登花 - Task.26
	 */
	/** 備考 */
	@Size(max = 100, message = "{maxlength}")
	private String note;

	
	
	/** セクション名 */
	private String sectionName;
	/** 当日フラグ */
	private Boolean isToday;
	/** エラーフラグ */
	private Boolean isError;
	/** 日付（画面表示用） */
	private String dispTrainingDate;
	/** ステータス（画面表示用） */
	private String statusDispName;
	/** LMSユーザーID */
	private String lmsUserId;
	/** ユーザー名 */
	private String userName;
	/** コース名 */
	private String courseName;
	/** インデックス */
	private String index;
}

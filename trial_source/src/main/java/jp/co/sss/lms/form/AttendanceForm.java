package jp.co.sss.lms.form;

import java.util.LinkedHashMap;
import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * 勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class AttendanceForm {

	/** LMSユーザーID */
	private Integer lmsUserId;
	/** グループID */
	private Integer groupId;
	/** 年間計画No */
	private String nenkanKeikakuNo;
	/** ユーザー名 */
	private String userName;
	/** 退校フラグ */
	private Integer leaveFlg;
	/** 退校日 */
	private String leaveDate;
	/** 退校日（表示用） */
	private String dispLeaveDate;
	/** 中抜け時間(プルダウン) */
	private LinkedHashMap<Integer, String> blankTimes;
	
	
	// 河島麻登花 – Task.26
	// 勤怠入力用プルダウン
	private LinkedHashMap<Integer, String> hourMap;    // 出勤退勤(時)
	private LinkedHashMap<Integer, String> minuteMap;  // 出勤退勤(分)
	
	// 河島麻登花 – Task.27
	@Valid
	private List<DailyAttendanceForm> attendanceList;  // 日次勤怠フォームリスト
}


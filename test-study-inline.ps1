# 内联集成测试：课程学习页 5 个接口
$BaseUrl = "http://localhost:8080"
$Username = "student02"
$Password = "123456"

# 清空历史进度
$env:MYSQL_PWD = "127315jj"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -D nursing `
    -e "DELETE FROM user_course_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_course_point_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_course_resource_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_learning_record WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');" 2>&1 | Out-Null
Write-Host "=== 已清空历史进度 ===" -ForegroundColor Gray

# 登录
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
$LoginResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -Headers @{"Content-Type"="application/json"}
$Token = $LoginResp.data.accessToken
Write-Host "登录成功 user=$($LoginResp.data.user.username) id=$($LoginResp.data.user.id) tokenLen=$($Token.Length)" -ForegroundColor Green
$Headers = @{ Authorization = "Bearer $Token"; "Content-Type" = "application/json" }

function Call($method, $url, $body, $desc) {
    Write-Host "`n>>> $desc <<< [$method $url]" -ForegroundColor Cyan
    try {
        if ($body -eq $null) {
            $r = Invoke-RestMethod -Uri $url -Headers $Headers -Method $method -ErrorAction Stop
        } else {
            $json = $body | ConvertTo-Json -Compress -Depth 5
            $r = Invoke-RestMethod -Uri $url -Headers $Headers -Method $method -Body $json -ErrorAction Stop
        }
        Write-Host "OK  code=$($r.code) message=$($r.message)" -ForegroundColor Green
        $r.data | ConvertTo-Json -Depth 8
        return $r
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { -1 }
        $stream = $_.Exception.Response.GetResponseStream()
        $raw = (New-Object System.IO.StreamReader($stream)).ReadToEnd()
        try { $err = $raw | ConvertFrom-Json } catch { $err = $raw }
        Write-Host "HTTP=$status code=$($err.code) message=$($err.message)" -ForegroundColor Red
        if ($err.data) { $err.data | ConvertTo-Json -Depth 8 } else { $raw }
        return $err
    }
}

# ========== 接口 1: 查询课程学习页 ==========
Call GET "$BaseUrl/api/learner/courses/101/points/10002/study?activeType=VIDEO" $null "接口1：查询课程学习页 (course=101, point=10002)"

# ========== 接口 2-1: 视频进度 首次50秒 (未完成) ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10002/videos/501/progress" `
    @{ currentSeconds=50; durationSeconds=516; eventType="PLAY"; ended=$false } `
    "接口2-1：视频进度 50s -> LEARNING"

# ========== 接口 2-2: 视频进度 491s(95%) 触发完成 ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10002/videos/501/progress" `
    @{ currentSeconds=491; durationSeconds=516; eventType="AUTO"; ended=$false } `
    "接口2-2：视频进度 491s (>=95%) -> COMPLETED"

# ========== 接口 3: 文章完成 ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10002/articles/701/complete" $null "接口3：标记文章701完成 (第1次)"
Call POST "$BaseUrl/api/learner/courses/101/points/10002/articles/701/complete" $null "接口3：标记文章701完成 (第2次 幂等)"

# ========== 接口 4: PPT完成 ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10002/ppts/801/complete" $null "接口4：标记PPT801完成"

# ========== 接口 5: 课程点完成 (此时 point=10002 视频/文章/PPT都已完成，应成功) ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10002/complete" $null "接口5：标记课程点10002完成"

# ========== 接口 5b: 错误场景 point=10003 视频未完成 应返回6306 ==========
Call POST "$BaseUrl/api/learner/courses/101/points/10003/complete" $null "接口5-错误：point=10003仍有未完成视频，应返回6306+unfinishedResources"

# ========== 数据库校验 ==========
Write-Host "`n`n========= 最终数据库状态 =========" -ForegroundColor Green
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -D nursing -t `
    -e "SET @uid=(SELECT id FROM user WHERE username='$Username');
SELECT 'A_user_course_progress' AS tbl, status, progress_percent AS pct, last_point_id AS lastPt, completed_at IS NOT NULL AS done FROM user_course_progress WHERE user_id=@uid AND course_id=101;
SELECT 'B_user_course_point_progress' AS tbl, course_point_id AS ptId, status, completed_at IS NOT NULL AS done FROM user_course_point_progress WHERE user_id=@uid AND course_id=101 ORDER BY course_point_id;
SELECT 'C_user_course_resource_progress' AS tbl, resource_type AS rt, resource_id AS rid, status, progress_percent AS pct, max_position_seconds AS maxPos FROM user_course_resource_progress WHERE user_id=@uid AND course_id=101 ORDER BY resource_type, resource_id;
SELECT 'D_user_learning_record' AS tbl, id, action_type AS act, resource_type AS rt, resource_id AS rid, title FROM user_learning_record WHERE user_id=@uid AND course_id=101 ORDER BY id;" 2>&1

Write-Host "`n=== 全部测试结束 ===" -ForegroundColor Green

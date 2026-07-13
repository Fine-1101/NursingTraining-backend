# 学员端课程学习页 - 集成测试脚本 (兼容 PowerShell 5.1，不使用 -SkipHttpErrorCheck)
$BaseUrl = "http://localhost:8080"
$Username = "student02"
$Password = "123456"

function JwtHeader($token) {
    return @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
}

function ReadResponse($req) {
    try {
        if ($_.Exception -and $_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
            $reader.Close()
            try { return $body | ConvertFrom-Json } catch { return $body }
        }
    } catch {}
    return $null
}

function Check-Get($url, $headers, $desc) {
    Write-Host "`n===== GET $url  ($desc) =====" -ForegroundColor Cyan
    try {
        $resp = Invoke-RestMethod -Uri $url -Headers $headers -Method Get -ErrorAction Stop
        Write-Host "response.code = $($resp.code)" -ForegroundColor Yellow
        Write-Host ($resp | ConvertTo-Json -Depth 6)
        return $resp
    } catch {
        $err = ReadResponse $_
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        Write-Host "HTTP=$status  response=$($err | ConvertTo-Json -Depth 6 -ErrorAction SilentlyContinue)" -ForegroundColor Red
        return $err
    }
}

function Check-Post($url, $headers, $body, $desc) {
    Write-Host "`n===== POST $url  ($desc) =====" -ForegroundColor Cyan
    try {
        if ($body -eq $null) {
            $resp = Invoke-RestMethod -Uri $url -Headers $headers -Method Post -ErrorAction Stop
        } else {
            $json = $body | ConvertTo-Json -Compress
            $resp = Invoke-RestMethod -Uri $url -Headers $headers -Method Post -Body $json -ErrorAction Stop
        }
        Write-Host "response.code = $($resp.code)" -ForegroundColor Yellow
        Write-Host ($resp | ConvertTo-Json -Depth 6)
        return $resp
    } catch {
        $err = ReadResponse $_
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        Write-Host "HTTP=$status" -ForegroundColor Red
        Write-Host ($err | ConvertTo-Json -Depth 6 -ErrorAction SilentlyContinue)
        return $err
    }
}

# ========== Step 0: 登录 ==========
Write-Host "`n#######################################" -ForegroundColor Green
Write-Host "# Step 0: 登录获取 JWT ($Username/$Password)" -ForegroundColor Green
Write-Host "#######################################" -ForegroundColor Green

$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
try {
    $LoginResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post `
        -Headers @{ "Content-Type" = "application/json" } -Body $loginBody -ErrorAction Stop
} catch {
    Write-Host "登录失败，尝试注册..." -ForegroundColor Yellow
    $regBody = @{ username=$Username; password=$Password; realName="集成测试学员"; deptId=1; roleType=1 } | ConvertTo-Json -Compress
    try {
        $reg = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post `
            -Headers @{ "Content-Type" = "application/json" } -Body $regBody -ErrorAction Stop
        Write-Host ($reg | ConvertTo-Json)
    } catch {
        Write-Host "注册异常: $($_.Exception.Message)" -ForegroundColor Red
    }
    try {
        $LoginResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post `
            -Headers @{ "Content-Type" = "application/json" } -Body $loginBody -ErrorAction Stop
    } catch {
        Write-Host "登录彻底失败: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
if ($LoginResp.code -ne 0) {
    Write-Host "登录失败(code=$($LoginResp.code)): $($LoginResp.message)" -ForegroundColor Red; exit 1
}
$Token = $LoginResp.data.token
Write-Host "登录成功，token前缀=$($Token.Substring(0, [Math]::Min(30, $Token.Length)))..." -ForegroundColor Green
$Headers = JwtHeader $Token

# ========== 2. 测试 5 个接口 ==========
$courseId = 101
$pointId  = 10002
$videoId  = 501
$articleId = 701
$pptId    = 801

# 清空旧进度
$env:MYSQL_PWD = "127315jj"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -D nursing `
    -e "DELETE FROM user_course_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_course_point_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_course_resource_progress WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');
        DELETE FROM user_learning_record WHERE course_id=101 AND user_id=(SELECT id FROM user WHERE username='$Username');" 2>&1 | Out-Null
Write-Host "已清空用户 $Username 在课程101下的历史进度与记录。" -ForegroundColor Gray

# (1) 查询学习页
Check-Get -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/study?activeType=VIDEO" `
    -headers $Headers -desc "接口1：查询课程学习页"

# (2)-1 视频进度第一次
Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/videos/$videoId/progress" `
    -headers $Headers -desc "接口2-1：首次保存视频进度（50秒，未完成）" `
    -body @{ currentSeconds=50; durationSeconds=516; eventType="PLAY"; ended=$false }

# (2)-2 视频进度达到95%
Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/videos/$videoId/progress" `
    -headers $Headers -desc "接口2-2：保存视频进度491/516秒（95%+ -> 触发完成）" `
    -body @{ currentSeconds=491; durationSeconds=516; eventType="AUTO"; ended=$false }

# (3) 文章完成
Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/articles/$articleId/complete" `
    -headers $Headers -desc "接口3：标记文章完成" -body $null

Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/articles/$articleId/complete" `
    -headers $Headers -desc "接口3：重复标记文章完成（幂等验证）" -body $null

# (4) PPT完成
Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/ppts/$pptId/complete" `
    -headers $Headers -desc "接口4：标记PPT完成" -body $null

# (5) 课程点完成
Check-Post -url "$BaseUrl/api/learner/courses/$courseId/points/$pointId/complete" `
    -headers $Headers -desc "接口5：标记课程点完成" -body $null

# (5b) 错误场景：point=10003还有视频未完成，应返回6306
Check-Post -url "$BaseUrl/api/learner/courses/101/points/10003/complete" `
    -headers $Headers -desc "接口5-错误场景：point=10003还有视频未完成应返回6306带unfinishedResources" -body $null

# ========== 3. 校验数据库 ==========
Write-Host "`n#######################################" -ForegroundColor Green
Write-Host "# Step 3: 校验数据库结果" -ForegroundColor Green
Write-Host "#######################################" -ForegroundColor Green

& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -D nursing -t `
    -e "SET @uid=(SELECT id FROM user WHERE username='$Username');
        SELECT 'user_course_progress' AS tbl, status, progress_percent, last_point_id, completed_at IS NOT NULL AS completed FROM user_course_progress WHERE user_id=@uid AND course_id=101;
        SELECT 'user_course_point_progress' AS tbl, course_point_id, status, completed_at IS NOT NULL AS completed FROM user_course_point_progress WHERE user_id=@uid AND course_id=101 ORDER BY course_point_id;
        SELECT 'user_course_resource_progress' AS tbl, resource_type, resource_id, status, progress_percent, max_position_seconds FROM user_course_resource_progress WHERE user_id=@uid AND course_id=101 ORDER BY resource_type, resource_id;
        SELECT 'user_learning_record' AS tbl, action_type, resource_type, resource_id, title FROM user_learning_record WHERE user_id=@uid AND course_id=101 ORDER BY id;" 2>&1

Write-Host "`n================== 测试脚本结束 ==================" -ForegroundColor Green

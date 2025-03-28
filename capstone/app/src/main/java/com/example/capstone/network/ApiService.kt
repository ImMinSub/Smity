package com.example.capstone.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// API 그룹 매칭 인터페이스
interface ApiService {
  @POST("find_group")
  suspend fun findGroup( @Body request : FindGroupRequest ) : Response<FindGroupResponse>

  @POST("login")
  suspend fun login( @Body request : Login ) : Response<Login>

  @POST("")
}



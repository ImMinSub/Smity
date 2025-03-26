package com.example.capstone.di  // 의존성 주입 관련 패키지

import android.content.Context  // 안드로이드 컨텍스트를 사용하기 위한 임포트
import androidx.room.Room  // Room 데이터베이스 빌더를 사용하기 위한 임포트
import com.example.capstone.db.AppDatabase  // 앱 데이터베이스 클래스 임포트
import com.example.capstone.db.ServerDao  // 서버 DAO 인터페이스 임포트
import com.example.capstone.db.UserDao  // 사용자 DAO 인터페이스 임포트
import dagger.Module  // Dagger 모듈 어노테이션 임포트
import dagger.Provides  // Dagger 프로바이더 어노테이션 임포트
import dagger.hilt.InstallIn  // Hilt 설치 어노테이션 임포트
import dagger.hilt.android.qualifiers.ApplicationContext  // 애플리케이션 컨텍스트 한정자 임포트
import dagger.hilt.components.SingletonComponent  // 싱글톤 컴포넌트 임포트
import javax.inject.Singleton  // 싱글톤 스코프 어노테이션 임포트

@Module  // Dagger 모듈로 지정
@InstallIn(SingletonComponent::class)  // 싱글톤 컴포넌트에 설치됨
object DatabaseModule {  // 데이터베이스 의존성을 제공하는 모듈 객체

    @Provides  // Dagger에게 이 메서드가 의존성을 제공한다고 알림
    @Singleton  // 애플리케이션 생명주기 동안 단일 인스턴스로 유지
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {  // 데이터베이스 인스턴스를 제공하는 함수
        return Room.databaseBuilder(  // Room 데이터베이스 빌더 사용
            appContext,  // 애플리케이션 컨텍스트 제공
            AppDatabase::class.java,  // 데이터베이스 클래스 지정
            "capstone.db"  // 데이터베이스 파일 이름 설정
        ).build()  // 데이터베이스 인스턴스 생성
    }

    @Provides  // Dagger에게 이 메서드가 의존성을 제공한다고 알림
    fun provideUserDao(database: AppDatabase): UserDao {  // UserDao 인스턴스를 제공하는 함수
        return database.userDao()  // 데이터베이스로부터 UserDao 인스턴스 반환
    }

    @Provides  // Dagger에게 이 메서드가 의존성을 제공한다고 알림
    fun provideServerDao(database: AppDatabase): ServerDao {  // ServerDao 인스턴스를 제공하는 함수
        return database.serverDao()  // 데이터베이스로부터 ServerDao 인스턴스 반환
    }
} 

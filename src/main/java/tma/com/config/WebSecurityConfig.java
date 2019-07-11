package tma.com.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private DataSource dataSource;
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		return bCryptPasswordEncoder;
	}
	
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		// Cài đặt dịch vụ tìm kiếm User trong Database.
		// Cài đặt PasswordEncoder.
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http
			.csrf().disable()
		
			.authorizeRequests()
				// Các trang không cần quyền.
				.antMatchers("/", "/login", "/logout").permitAll()
				
				// Trang /uerInfo yêu cầu phải logiin với vai trò ROLE_USER hoặc ROLE_ADMIN.
				// Nếu chưa login sẽ redirect tới trang /login.
				.antMatchers("/userInfo").access("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
				
				// Trang cần quyền Admin
				.antMatchers("/admin").access("hasRole('ROLE_ADMIN')")
				
				// Ngoại lệ khi truy cập trang không đươc cấp quyền
				.and().exceptionHandling().accessDeniedPage("/403")
		
				// Cấu hình Login Form
				.and().formLogin()
					// Submit URL của trang login
					.loginProcessingUrl("/j_spring_security_check")
					.loginPage("/login")
					.defaultSuccessUrl("/userAccountInfo")
					.failureUrl("/login?error=true")
					.usernameParameter("username")
					.passwordParameter("password")
					// Cấu hình cho Logout Page.
					.and().logout().logoutUrl("/logout").logoutSuccessUrl("/logoutSuccessful")
				
				// Cấu hình cho lưu trữ đăng nhập (Remenber Me)
				.and()
					.rememberMe().tokenRepository(this.persistentTokenRepository())
					.tokenValiditySeconds(1 * 24 * 60 * 60);
	}
	
	@Bean
	public PersistentTokenRepository persistentTokenRepository( ) {
		JdbcTokenRepositoryImpl db = new JdbcTokenRepositoryImpl();
		db.setDataSource(dataSource);
		return db;
	}
}

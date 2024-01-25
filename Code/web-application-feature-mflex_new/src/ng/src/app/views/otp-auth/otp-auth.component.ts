import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { LoginSharedService } from '../service/login-shared.service';
import { LoginService } from '../service/login.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-otp-auth',
  templateUrl: './otp-auth.component.html',
  styleUrls: ['./otp-auth.component.css']
})
export class OtpAuthComponent {

  OTPAuth: FormGroup;
  errormsg: boolean = true;
  receivedRequestID: any;
  refresh_token: string | undefined;
  constructor(private formBuilder: FormBuilder, private dataService: LoginSharedService, private loginApiService: LoginService, private router: Router) {
    this.OTPAuth = this.formBuilder.group({
      otpAuth: ['', [Validators.required]]
    });
  }
  ngOnInit() {
    this.dataService.serviceID$.subscribe(data => {
      this.receivedRequestID = data;
    });
  }

  verifyOTP() {
    const OTP = this.OTPAuth.get('otpAuth')?.value;
    console.log('OTP :', OTP);
    console.log('Service request ID from OTP :', this.receivedRequestID);
    this.loginApiService.getOtp('LOGIN_POLICY','OTP01').subscribe((data: any) => {

      this.loginApiService.validateOTP(OTP, data.serviceRequestId).subscribe(
        (response) => {
          this.refresh_token = response.token.refresh_token;
          localStorage.setItem('token', JSON.stringify(this.refresh_token));
          console.log('Validate OTP success response: ', response)
          this.router.navigate(['dashboard']);
        }, (error) => {
          console.log('Validate OTP error response: ', error)
          // alert(error.errors[0].message);
          alert('Invalid OTP. Try again');
        }
      )

    })

  }
}

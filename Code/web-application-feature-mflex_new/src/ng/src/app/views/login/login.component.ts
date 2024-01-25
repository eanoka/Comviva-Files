import { Component } from '@angular/core';
import { LoginService } from '../service/login.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { LoginSharedService } from '../service/login-shared.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  login: FormGroup;
  errormsg: boolean = true;
  isPasswordVisible: boolean = false;
  refresh_token: string | undefined;
  constructor(private formBuilder: FormBuilder, private loginApiService: LoginService, private dataService: LoginSharedService, private router: Router) {
    this.login = this.formBuilder.group({
      userID: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  submitLogin() {
    const userID = this.login.get('userID')?.value;
    const password = this.login.get('password')?.value;
    console.log('user ID:', userID);
    console.log('Password:', password);

    this.loginApiService.getToken().subscribe(
      (response) => {
        console.log('Token API response: ', response);
        const accessToken = response.access_token;
        localStorage.setItem('access_token',accessToken)
        console.log('access token value: ', accessToken);
        localStorage.setItem('userID', userID);
        this.loginApiService.getWeb(accessToken, userID, password).subscribe(
          (res) => {
            console.log('Fetch web API response: ', res);
            const status = res.status;
            const serviceRequestID = res.serviceRequestId;
            console.log('Status: ', status);
            console.log('Service Request ID: ', serviceRequestID);
            if (status == 'PAUSED') {
              this.router.navigate(['otp-auth']);
              this.dataService.setServiceID(serviceRequestID)
            }
            else if (status == 'SUCCEEDED') {
              this.refresh_token = res.token.refresh_token;
              localStorage.setItem('token', JSON.stringify(this.refresh_token));
              this.router.navigate(['dashboard']);
              alert('Logged in successfully');
            }
          }, (err) => {
            console.log('Fetch web API error: ', err)
            const errorMsg= err.errorUserMsg;
            alert('Invalid credentials. Try again');
          }
        )

      },
      (error) => {
        console.log('Token API error: ', error);
        alert('Login authentication is unsuccessfull');
      }
    );
  }

}

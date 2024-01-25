import { Component } from '@angular/core';
import { ChangepaswwordService } from '../service/changepassword.service';
import { LoginSharedService } from '../service/login-shared.service';

@Component({
  selector: 'app-new-password',
  templateUrl: './new-password.component.html',
  styleUrls: ['./new-password.component.css']
})
export class NewPasswordComponent {

  newPassword:string=''
  confirmNewPassword:string=''
  receivedRequestID: any;

constructor(public changepaswwordService:ChangepaswwordService,private dataService: LoginSharedService){

}


ngOnInit() {
  this.dataService.serviceID$.subscribe(data => {
    console.log('data------------->',data)
    this.receivedRequestID = data;
  });
}

  onChangePassword(newPassword:string,confirmNewPassword:string){



    
    this.changepaswwordService.changeSelfCredential(newPassword,confirmNewPassword,this.receivedRequestID).subscribe(()=>{



    })

  }

}

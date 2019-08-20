package com.abc.wechat.dao.db2;

import com.abc.wechat.entity.Contact;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ContactsMapper {
    List<Contact> selectAllContacts();
    String selectNameById(String UserName);
}

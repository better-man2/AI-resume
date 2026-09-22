/**
 * @projectName springAi
 * @package com.shanzhu.biographical.model
 * @className com.shanzhu.biographical.model.ChatRequest
 * @copyright Copyright 2024 Thunisoft, Inc All rights reserved.
 */
package com.shanzhu.biographical.model;


import lombok.Data;

@Data
public class ChatRequest {
    private String type;
    private String text;
    private String msg;
    private String userId;
    private String username;
}
